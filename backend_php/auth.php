<?php
/**
 * NOVA STORE - Authentication & Security Engine
 * Location: /nova/auth.php
 */

require_once __DIR__ . '/config.php';

/**
 * Get all incoming HTTP request headers safely across Apache, Nginx, LiteSpeed, CGI
 * @return array
 */
function getRequestHeaders() {
    if (function_exists('apache_request_headers')) {
        $headers = apache_request_headers();
        if ($headers !== false) {
            $normalized = [];
            foreach ($headers as $k => $v) {
                $normalized[strtolower($k)] = $v;
            }
            return $normalized;
        }
    }

    $headers = [];
    foreach ($_SERVER as $key => $value) {
        if (substr($key, 0, 5) === 'HTTP_') {
            $header = str_replace(' ', '-', strtolower(str_replace('_', ' ', substr($key, 5))));
            $headers[$header] = $value;
        } elseif ($key === 'CONTENT_TYPE') {
            $headers['content-type'] = $value;
        } elseif ($key === 'CONTENT_LENGTH') {
            $headers['content-length'] = $value;
        }
    }
    return $headers;
}

/**
 * Extract API Key from request headers
 * Checks X-API-Key or Bearer API token
 * @return string|null
 */
function extractApiKey() {
    $headers = getRequestHeaders();

    // Check X-API-Key header
    if (!empty($headers['x-api-key'])) {
        return trim($headers['x-api-key']);
    }

    // Check Authorization header for API Key fallback
    if (!empty($headers['authorization'])) {
        $parts = explode(' ', trim($headers['authorization']), 2);
        if (count($parts) === 2 && strcasecmp($parts[0], 'Bearer') === 0) {
            return trim($parts[1]);
        }
    }

    return null;
}

/**
 * Extract User Session Token from Authorization header
 * Header: Authorization: Bearer <USER_TOKEN>
 * @return string|null
 */
function extractUserToken() {
    $headers = getRequestHeaders();

    if (!empty($headers['authorization'])) {
        $parts = explode(' ', trim($headers['authorization']), 2);
        if (count($parts) === 2 && strcasecmp($parts[0], 'Bearer') === 0) {
            return trim($parts[1]);
        }
    }

    // Also allow custom header X-User-Token if proxy strips Authorization
    if (!empty($headers['x-user-token'])) {
        return trim($headers['x-user-token']);
    }

    return null;
}

/**
 * Validate Application API Key
 */
function requireApiKey() {
    global $ALLOWED_API_KEYS;
    $headers = getRequestHeaders();

    $key = null;
    if (!empty($headers['x-api-key'])) {
        $key = trim($headers['x-api-key']);
    }

    // If X-API-Key not provided, check Authorization if it's not a user token
    if ($key === null && !empty($headers['authorization'])) {
        $parts = explode(' ', trim($headers['authorization']), 2);
        if (count($parts) === 2 && strcasecmp($parts[0], 'Bearer') === 0) {
            $candidate = trim($parts[1]);
            if (in_array($candidate, $ALLOWED_API_KEYS, true)) {
                $key = $candidate;
            }
        }
    }

    // In testing/dev, if allowed keys are set, verify
    if (!empty($ALLOWED_API_KEYS)) {
        if ($key === null || !in_array($key, $ALLOWED_API_KEYS, true)) {
            sendJsonResponse(false, null, 'غير مصرح: مفتاح الـ API غير صالح أو مفقود (X-API-Key مطلوب)', 401);
        }
    }
}

/**
 * Hash raw token with SHA-256 for secure database storage
 * @param string $rawToken
 * @return string
 */
function hashUserToken($rawToken) {
    return hash('sha256', $rawToken);
}

/**
 * Create a new user session token in user_tokens table
 * @param int $userId
 * @param string $deviceName
 * @return string The raw token to be sent to Android client
 */
function createUserSession($userId, $deviceName = 'Android Device') {
    $pdo = getDbConnection();

    // Generate 64-char cryptographically secure random token
    $rawToken = bin2hex(random_bytes(32));
    $tokenHash = hashUserToken($rawToken);

    $expiresAt = date('Y-m-d H:i:s', strtotime('+' . TOKEN_EXPIRY_DAYS . ' days'));

    $stmt = $pdo->prepare("
        INSERT INTO user_tokens (user_id, token_hash, device_name, expires_at, created_at, last_used_at)
        VALUES (?, ?, ?, ?, NOW(), NOW())
    ");
    $stmt->execute([$userId, $tokenHash, substr($deviceName, 0, 150), $expiresAt]);

    // Update user's last_login_at
    $updateStmt = $pdo->prepare("UPDATE users SET last_login_at = NOW() WHERE id = ?");
    $updateStmt->execute([$userId]);

    return $rawToken;
}

/**
 * Authenticate incoming User Token and return User data
 * @return array Authenticated user data (id, name, email, phone, created_at, etc.)
 */
function requireAuthenticatedUser() {
    $rawToken = extractUserToken();

    if (empty($rawToken)) {
        sendJsonResponse(false, null, 'تسجيل الدخول مطلوب للوصول إلى هذه الخدمة', 401);
    }

    $tokenHash = hashUserToken($rawToken);
    $pdo = getDbConnection();

    $stmt = $pdo->prepare("
        SELECT 
            t.id AS session_token_id,
            t.user_id,
            u.id,
            u.name,
            u.email,
            u.phone,
            u.status,
            u.created_at,
            u.updated_at,
            u.last_login_at
        FROM user_tokens t
        INNER JOIN users u ON t.user_id = u.id
        WHERE t.token_hash = ?
          AND t.revoked_at IS NULL
          AND t.expires_at > NOW()
        LIMIT 1
    ");
    $stmt->execute([$tokenHash]);
    $user = $stmt->fetch();

    if (!$user) {
        sendJsonResponse(false, null, 'جلسة المستخدم منتهية الصلاحية أو غير صالحة، يرجى تسجيل الدخول مجدداً', 401);
    }

    if ($user['status'] === 'deleted') {
        sendJsonResponse(false, null, 'هذا الحساب تم حذفه مسبقاً', 403);
    }

    if ($user['status'] === 'suspended') {
        sendJsonResponse(false, null, 'هذا الحساب معطل حالياً من قبل الإدارة', 403);
    }

    // Update last_used_at asynchronously/efficiently
    $updateUsage = $pdo->prepare("UPDATE user_tokens SET last_used_at = NOW() WHERE id = ?");
    $updateUsage->execute([$user['session_token_id']]);

    // Format IDs as strings or clean numbers
    $user['id'] = (string)$user['id'];
    unset($user['session_token_id'], $user['user_id']);

    return $user;
}

/**
 * Revoke current user session token
 * @return bool
 */
function revokeCurrentSession() {
    $rawToken = extractUserToken();
    if (empty($rawToken)) {
        return false;
    }

    $tokenHash = hashUserToken($rawToken);
    $pdo = getDbConnection();

    $stmt = $pdo->prepare("UPDATE user_tokens SET revoked_at = NOW() WHERE token_hash = ?");
    return $stmt->execute([$tokenHash]);
}

/**
 * Get Request JSON Body as Associative Array
 * @return array
 */
function getJsonInput() {
    $raw = file_get_contents('php://input');
    if (empty($raw)) {
        return [];
    }
    $decoded = json_decode($raw, true);
    return is_array($decoded) ? $decoded : [];
}

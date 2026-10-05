<?php
/**
 * NOVA STORE - Main REST API Controller
 * URL: https://nova1.ct.ws/nova/api.php
 */

require_once __DIR__ . '/config.php';
require_once __DIR__ . '/auth.php';

// Determine requested endpoint
$endpoint = isset($_GET['endpoint']) ? trim($_GET['endpoint']) : '';
$method = $_SERVER['REQUEST_METHOD'];

// Handle CORS OPTIONS preflight
if ($method === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Global API Key validation (ensures only authorized apps/clients access the API)
requireApiKey();

$pdo = getDbConnection();

switch ($endpoint) {

    // ====================================================
    // HEALTH CHECK
    // ====================================================
    case 'health':
        $dbStatus = 'connected';
        try {
            $pdo->query('SELECT 1');
        } catch (Exception $e) {
            $dbStatus = 'disconnected';
        }

        sendJsonResponse(true, [
            'status' => 'online',
            'store_name' => 'NOVA STORE',
            'version' => '2.1.0',
            'database' => $dbStatus,
            'timestamp' => date('c'),
            'server_time' => date('Y-m-d H:i:s')
        ], 'خادم متجر نوفا يعمل بصورة طبيعية');
        break;

    // ====================================================
    // STORE SETTINGS
    // ====================================================
    case 'settings':
        sendJsonResponse(true, [
            'store_name' => 'NOVA STORE',
            'currency' => 'USD',
            'currency_symbol' => '$',
            'support_email' => 'support@novastore.com',
            'support_phone' => '+966500000000',
            'shipping_fee' => 15.00,
            'free_shipping_threshold' => 150.00,
            'features' => [
                'user_registration' => true,
                'order_tracking' => true,
                'guest_checkout' => false,
                'promotions' => true
            ]
        ]);
        break;

    // ====================================================
    // CATEGORIES LIST
    // ====================================================
    case 'categories':
        $stmt = $pdo->prepare("
            SELECT 
                c.id, 
                c.name, 
                c.slug, 
                c.icon_url,
                COUNT(p.id) AS products_count
            FROM categories c
            LEFT JOIN products p ON c.id = p.category_id
            GROUP BY c.id
            ORDER BY c.sort_order ASC, c.id ASC
        ");
        $stmt->execute();
        $categories = $stmt->fetchAll();

        sendJsonResponse(true, $categories);
        break;

    // ====================================================
    // PRODUCTS LIST & SEARCH
    // ====================================================
    case 'products':
        $categoryId = isset($_GET['category_id']) ? (int)$_GET['category_id'] : 0;
        $search = isset($_GET['q']) ? trim($_GET['q']) : '';
        $featuredOnly = isset($_GET['featured']) && $_GET['featured'] == '1';

        $sql = "
            SELECT 
                p.id, 
                p.name, 
                p.description, 
                p.price, 
                p.original_price, 
                p.image_url, 
                p.stock, 
                p.rating, 
                p.is_featured,
                c.id AS category_id,
                c.name AS category_name
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE 1=1
        ";
        $params = [];

        if ($categoryId > 0) {
            $sql .= " AND p.category_id = ?";
            $params[] = $categoryId;
        }

        if ($featuredOnly) {
            $sql .= " AND p.is_featured = 1";
        }

        if (!empty($search)) {
            $sql .= " AND (p.name LIKE ? OR p.description LIKE ?)";
            $params[] = '%' . $search . '%';
            $params[] = '%' . $search . '%';
        }

        $sql .= " ORDER BY p.id DESC LIMIT 100";

        $stmt = $pdo->prepare($sql);
        $stmt->execute($params);
        $products = $stmt->fetchAll();

        sendJsonResponse(true, $products);
        break;

    // ====================================================
    // SINGLE PRODUCT DETAILS
    // ====================================================
    case 'product':
        $id = isset($_GET['id']) ? (int)$_GET['id'] : 0;
        if ($id <= 0) {
            sendJsonResponse(false, null, 'معرّف المنتج غير صالح', 400);
        }

        $stmt = $pdo->prepare("
            SELECT 
                p.id, 
                p.name, 
                p.description, 
                p.price, 
                p.original_price, 
                p.image_url, 
                p.stock, 
                p.rating, 
                p.is_featured,
                c.id AS category_id,
                c.name AS category_name
            FROM products p
            LEFT JOIN categories c ON p.category_id = c.id
            WHERE p.id = ?
            LIMIT 1
        ");
        $stmt->execute([$id]);
        $product = $stmt->fetch();

        if (!$product) {
            sendJsonResponse(false, null, 'المنتج غير موجود', 404);
        }

        sendJsonResponse(true, $product);
        break;

    // ====================================================
    // USER REGISTRATION
    // ====================================================
    case 'register':
        if ($method !== 'POST') {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }

        $input = getJsonInput();
        $name = isset($input['name']) ? trim($input['name']) : '';
        $email = isset($input['email']) ? trim(strtolower($input['email'])) : '';
        $phone = isset($input['phone']) ? trim($input['phone']) : '';
        $password = isset($input['password']) ? (string)$input['password'] : '';
        $deviceName = isset($input['device_name']) ? trim($input['device_name']) : 'Android Device';

        // 1. Validation
        if (mb_strlen($name) < 2) {
            sendJsonResponse(false, null, 'يرجى إدخال اسم مستخدم صحيح لا يقل عن حرفين', 422);
        }

        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) {
            sendJsonResponse(false, null, 'يرجى إدخال بريد إلكتروني صحيح ومعتمد', 422);
        }

        if (strlen($password) < 6) {
            sendJsonResponse(false, null, 'كلمة المرور يجب أن تتكون من 6 خانات على الأقل', 422);
        }

        // 2. Check if email already exists
        $checkStmt = $pdo->prepare("SELECT id, status FROM users WHERE email = ? LIMIT 1");
        $checkStmt->execute([$email]);
        $existing = $checkStmt->fetch();

        if ($existing) {
            if ($existing['status'] === 'deleted') {
                sendJsonResponse(false, null, 'هذا البريد الإلكتروني مسجل لحساب تم حذفه مسبقاً، يرجى التواصل مع الدعم', 409);
            }
            sendJsonResponse(false, null, 'البريد الإلكتروني مسجل بالفعل لمستخدم آخر', 409);
        }

        // 3. Hash password securely using standard Bcrypt / Argon2
        $passwordHash = password_hash($password, PASSWORD_DEFAULT);

        // 4. Insert new user using Prepared Statement
        $insertStmt = $pdo->prepare("
            INSERT INTO users (name, email, phone, password_hash, status, created_at, updated_at)
            VALUES (?, ?, ?, ?, 'active', NOW(), NOW())
        ");
        $insertStmt->execute([$name, $email, $phone, $passwordHash]);
        $newUserId = (int)$pdo->lastInsertId();

        // 5. Generate secure session token and save hash in user_tokens
        $rawToken = createUserSession($newUserId, $deviceName);

        // 6. Return response conforming strictly to specification
        sendJsonResponse(true, [
            'user' => [
                'id' => (string)$newUserId,
                'name' => $name,
                'email' => $email,
                'phone' => $phone
            ],
            'token' => $rawToken
        ], 'تم إنشاء الحساب بنجاح', 201);
        break;

    // ====================================================
    // USER LOGIN
    // ====================================================
    case 'login':
        if ($method !== 'POST') {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }

        $input = getJsonInput();
        $email = isset($input['email']) ? trim(strtolower($input['email'])) : '';
        $password = isset($input['password']) ? (string)$input['password'] : '';
        $deviceName = isset($input['device_name']) ? trim($input['device_name']) : 'Android Device';

        if (empty($email) || empty($password)) {
            sendJsonResponse(false, null, 'يرجى إدخال البريد الإلكتروني وكلمة المرور', 422);
        }

        $stmt = $pdo->prepare("
            SELECT id, name, email, phone, password_hash, status 
            FROM users 
            WHERE email = ? 
            LIMIT 1
        ");
        $stmt->execute([$email]);
        $user = $stmt->fetch();

        if (!$user || !password_verify($password, $user['password_hash'])) {
            sendJsonResponse(false, null, 'البريد الإلكتروني أو كلمة المرور غير صحيحة', 401);
        }

        if ($user['status'] === 'deleted') {
            sendJsonResponse(false, null, 'تم إغلاق وحذف هذا الحساب', 403);
        }

        if ($user['status'] === 'suspended') {
            sendJsonResponse(false, null, 'تم تعطيل الحساب من قبل الإدارة', 403);
        }

        // Generate user token
        $rawToken = createUserSession((int)$user['id'], $deviceName);

        sendJsonResponse(true, [
            'user' => [
                'id' => (string)$user['id'],
                'name' => $user['name'],
                'email' => $user['email'],
                'phone' => $user['phone'] ?? ''
            ],
            'token' => $rawToken
        ], 'تم تسجيل الدخول بنجاح');
        break;

    // ====================================================
    // CURRENT USER PROFILE (ME)
    // ====================================================
    case 'me':
        $user = requireAuthenticatedUser();

        sendJsonResponse(true, [
            'user' => [
                'id' => (string)$user['id'],
                'name' => $user['name'],
                'email' => $user['email'],
                'phone' => $user['phone'] ?? '',
                'created_at' => $user['created_at'],
                'updated_at' => $user['updated_at'],
                'last_login_at' => $user['last_login_at']
            ]
        ]);
        break;

    // ====================================================
    // USER LOGOUT
    // ====================================================
    case 'logout':
        if ($method !== 'POST') {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }

        // Revoke the current active token
        revokeCurrentSession();

        sendJsonResponse(true, null, 'تم تسجيل الخروج بنجاح');
        break;

    // ====================================================
    // UPDATE PROFILE
    // ====================================================
    case 'profile_update':
        if ($method !== 'POST') {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }

        $user = requireAuthenticatedUser();
        $input = getJsonInput();

        $name = isset($input['name']) ? trim($input['name']) : '';
        $phone = isset($input['phone']) ? trim($input['phone']) : null;

        if (mb_strlen($name) < 2) {
            sendJsonResponse(false, null, 'الاسم يجب ألا يقل عن حرفين', 422);
        }

        // Security check: Only allow name and phone updates!
        $updateStmt = $pdo->prepare("
            UPDATE users 
            SET name = ?, phone = ?, updated_at = NOW() 
            WHERE id = ?
        ");
        $updateStmt->execute([$name, $phone, (int)$user['id']]);

        sendJsonResponse(true, [
            'user' => [
                'id' => (string)$user['id'],
                'name' => $name,
                'email' => $user['email'],
                'phone' => $phone ?? '',
                'updated_at' => date('Y-m-d H:i:s')
            ]
        ], 'تم تعديل البيانات بنجاح');
        break;

    // ====================================================
    // CHANGE PASSWORD
    // ====================================================
    case 'change_password':
        if ($method !== 'POST') {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }

        $user = requireAuthenticatedUser();
        $input = getJsonInput();

        $currentPassword = isset($input['current_password']) ? (string)$input['current_password'] : '';
        $newPassword = isset($input['new_password']) ? (string)$input['new_password'] : '';

        if (empty($currentPassword) || empty($newPassword)) {
            sendJsonResponse(false, null, 'يرجى إدخال كلمة المرور الحالية وكلمة المرور الجديدة', 422);
        }

        if (strlen($newPassword) < 6) {
            sendJsonResponse(false, null, 'كلمة المرور الجديدة يجب أن تتكون من 6 خانات على الأقل', 422);
        }

        // Fetch current password_hash from DB
        $stmt = $pdo->prepare("SELECT password_hash FROM users WHERE id = ? LIMIT 1");
        $stmt->execute([(int)$user['id']]);
        $row = $stmt->fetch();

        if (!$row || !password_verify($currentPassword, $row['password_hash'])) {
            sendJsonResponse(false, null, 'كلمة المرور الحالية غير صحيحة', 400);
        }

        // Hash new password
        $newPasswordHash = password_hash($newPassword, PASSWORD_DEFAULT);

        // Update password in DB
        $updateStmt = $pdo->prepare("
            UPDATE users 
            SET password_hash = ?, updated_at = NOW() 
            WHERE id = ?
        ");
        $updateStmt->execute([$newPasswordHash, (int)$user['id']]);

        // Revoke all other previous tokens for this user for security
        $revokeOthers = $pdo->prepare("
            UPDATE user_tokens 
            SET revoked_at = NOW() 
            WHERE user_id = ? AND token_hash != ?
        ");
        $rawToken = extractUserToken();
        $currentHash = $rawToken ? hashUserToken($rawToken) : '';
        $revokeOthers->execute([(int)$user['id'], $currentHash]);

        sendJsonResponse(true, null, 'تم تغيير كلمة المرور بنجاح');
        break;

    // ====================================================
    // DELETE ACCOUNT (SOFT DELETE)
    // ====================================================
    case 'delete_account':
        if ($method !== 'POST') {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }

        $user = requireAuthenticatedUser();

        // Perform safe soft delete
        $delStmt = $pdo->prepare("
            UPDATE users 
            SET status = 'deleted', updated_at = NOW() 
            WHERE id = ?
        ");
        $delStmt->execute([(int)$user['id']]);

        // Revoke all tokens for this user immediately
        $revokeStmt = $pdo->prepare("
            UPDATE user_tokens 
            SET revoked_at = NOW() 
            WHERE user_id = ?
        ");
        $revokeStmt->execute([(int)$user['id']]);

        sendJsonResponse(true, null, 'تم حذف الحساب بنجاح');
        break;

    // ====================================================
    // USER ORDERS (GET MY ORDERS / CREATE ORDER)
    // ====================================================
    case 'orders':
        $user = requireAuthenticatedUser();

        if ($method === 'GET') {
            $stmt = $pdo->prepare("
                SELECT 
                    o.id, 
                    o.total_amount, 
                    o.status, 
                    o.shipping_address, 
                    o.payment_method, 
                    o.created_at,
                    COUNT(oi.id) AS items_count
                FROM orders o
                LEFT JOIN order_items oi ON o.id = oi.order_id
                WHERE o.user_id = ?
                GROUP BY o.id
                ORDER BY o.id DESC
            ");
            $stmt->execute([(int)$user['id']]);
            $orders = $stmt->fetchAll();

            sendJsonResponse(true, $orders);
        } elseif ($method === 'POST') {
            $input = getJsonInput();
            $items = isset($input['items']) && is_array($input['items']) ? $input['items'] : [];
            $address = isset($input['shipping_address']) ? trim($input['shipping_address']) : 'العنوان الافتراضي';

            if (empty($items)) {
                sendJsonResponse(false, null, 'السلة فارغة، لا يمكن إنشاء طلب', 422);
            }

            $totalAmount = 0.0;
            foreach ($items as $item) {
                $totalAmount += ((float)($item['price'] ?? 0)) * ((int)($item['quantity'] ?? 1));
            }

            $pdo->beginTransaction();
            try {
                $orderStmt = $pdo->prepare("
                    INSERT INTO orders (user_id, total_amount, status, shipping_address, created_at)
                    VALUES (?, ?, 'pending', ?, NOW())
                ");
                $orderStmt->execute([(int)$user['id'], $totalAmount, $address]);
                $orderId = (int)$pdo->lastInsertId();

                $itemStmt = $pdo->prepare("
                    INSERT INTO order_items (order_id, product_id, quantity, unit_price)
                    VALUES (?, ?, ?, ?)
                ");
                foreach ($items as $it) {
                    $itemStmt->execute([
                        $orderId, 
                        (int)($it['product_id'] ?? $it['id']), 
                        (int)($it['quantity'] ?? 1), 
                        (float)($it['price'] ?? 0)
                    ]);
                }

                $pdo->commit();

                sendJsonResponse(true, [
                    'order_id' => $orderId,
                    'total_amount' => $totalAmount,
                    'status' => 'pending'
                ], 'تم إنشاء الطلب بنجاح', 201);
            } catch (Exception $e) {
                $pdo->rollBack();
                sendJsonResponse(false, null, 'فشل في حفظ الطلب', 500);
            }
        } else {
            sendJsonResponse(false, null, 'طريقة الطلب غير مسموح بها', 405);
        }
        break;

    // ====================================================
    // UNKNOWN ENDPOINT
    // ====================================================
    default:
        sendJsonResponse(false, null, 'نقطة النهاية المطلوبة غير موجودة (Endpoint not found)', 404);
        break;
}

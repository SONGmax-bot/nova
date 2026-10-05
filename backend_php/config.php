<?php
/**
 * NOVA STORE - Configuration & Database Connection
 * Location: /nova/config.php
 */

// Prevent direct output buffering issues
if (!ob_get_level()) {
    ob_start();
}

// Security headers & CORS
header('Content-Type: application/json; charset=utf-8');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, Authorization, X-API-Key, X-Requested-With');

// Handle preflight OPTIONS request
if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Error reporting: Production mode (hide detailed DB errors from public)
ini_set('display_errors', '0');
error_reporting(E_ALL & ~E_NOTICE & ~E_DEPRECATED);

// ========================================================
// 1. DATABASE CONFIGURATION
// Replace these with your actual InfinityFree / cPanel DB details:
// ========================================================
define('DB_HOST', getenv('DB_HOST') ?: 'sql205.infinityfree.com'); // or localhost
define('DB_NAME', getenv('DB_NAME') ?: 'if0_38000000_nova_store'); 
define('DB_USER', getenv('DB_USER') ?: 'if0_38000000'); 
define('DB_PASS', getenv('DB_PASS') ?: 'YOUR_DB_PASSWORD'); 
define('DB_CHARSET', 'utf8mb4');

// ========================================================
// 2. API KEYS CONFIGURATION
// Allowed application API keys for X-API-Key or Bearer API auth
// ========================================================
$ALLOWED_API_KEYS = [
    'NOVA_APP_KEY_2026_SECURE_98127391',
    'YOUR_API_KEY', // Default placeholder for backward compatibility
    'nova_mobile_app_prod_key_77a9',
];

// Token duration: 60 days
define('TOKEN_EXPIRY_DAYS', 60);

/**
 * Get PDO Database Connection
 * @return PDO
 */
function getDbConnection() {
    static $pdo = null;
    if ($pdo === null) {
        $dsn = "mysql:host=" . DB_HOST . ";dbname=" . DB_NAME . ";charset=" . DB_CHARSET;
        $options = [
            PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
            PDO::ATTR_EMULATE_PREPARES   => false,
            PDO::MYSQL_ATTR_INIT_COMMAND => "SET NAMES " . DB_CHARSET
        ];
        try {
            $pdo = new PDO($dsn, DB_USER, DB_PASS, $options);
        } catch (PDOException $e) {
            // NEVER reveal DB password or internal host to client
            sendJsonResponse(false, null, 'خطأ في الاتصال بقاعدة البيانات. يرجى مراجعة إعدادات الخادم.', 500);
        }
    }
    return $pdo;
}

/**
 * Standard JSON Response Helper
 */
function sendJsonResponse($ok, $data = null, $message = null, $statusCode = 200) {
    http_response_code($statusCode);
    $response = ['ok' => (bool)$ok];

    if ($message !== null) {
        if ($ok) {
            $response['message'] = $message;
        } else {
            $response['error'] = $message;
        }
    }

    if ($data !== null) {
        $response['data'] = $data;
    }

    echo json_encode($response, JSON_UNESCAPED_UNICODE | JSON_UNESCAPED_SLASHES);
    exit();
}

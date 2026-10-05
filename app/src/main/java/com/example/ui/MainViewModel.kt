package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferences
import com.example.data.local.SecureTokenManager
import com.example.data.local.database.CartItemEntity
import com.example.data.local.database.NovaDatabase
import com.example.data.model.CategoryDto
import com.example.data.model.HealthData
import com.example.data.model.OrderDto
import com.example.data.model.ProductDto
import com.example.data.model.UserDto
import com.example.data.remote.ApiClient
import com.example.data.repository.AuthRepository
import com.example.data.repository.AuthResult
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiMessage(val text: String, val isError: Boolean = false)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val appPreferences = AppPreferences(application)
    val secureTokenManager = SecureTokenManager(application)
    private val apiClient = ApiClient.getInstance(application)
    private val novaDb = NovaDatabase.getDatabase(application)

    val authRepo = AuthRepository(apiClient, secureTokenManager, appPreferences)
    val storeRepo = StoreRepository(apiClient, novaDb.cartDao())

    // Authentication States
    val currentUser: StateFlow<UserDto?> = authRepo.currentUser
    val isLoggedIn: StateFlow<Boolean> = authRepo.isLoggedIn

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    // Store & Products States
    private val _categories = MutableStateFlow<List<CategoryDto>>(emptyList())
    val categories: StateFlow<List<CategoryDto>> = _categories.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDto>>(emptyList())
    val products: StateFlow<List<ProductDto>> = _products.asStateFlow()

    private val _selectedCategory = MutableStateFlow<CategoryDto?>(null)
    val selectedCategory: StateFlow<CategoryDto?> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(false)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    private val _selectedProduct = MutableStateFlow<ProductDto?>(null)
    val selectedProduct: StateFlow<ProductDto?> = _selectedProduct.asStateFlow()

    // Cart Items from Room Database
    val cartItems: StateFlow<List<CartItemEntity>> = storeRepo.cartItemsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCartCount: StateFlow<Int?> = storeRepo.totalCartCountFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Orders State
    private val _orders = MutableStateFlow<List<OrderDto>>(emptyList())
    val orders: StateFlow<List<OrderDto>> = _orders.asStateFlow()

    private val _isLoadingOrders = MutableStateFlow(false)
    val isLoadingOrders: StateFlow<Boolean> = _isLoadingOrders.asStateFlow()

    // Health / Server Status
    private val _healthStatus = MutableStateFlow<HealthData?>(null)
    val healthStatus: StateFlow<HealthData?> = _healthStatus.asStateFlow()

    private val _isCheckingHealth = MutableStateFlow(false)
    val isCheckingHealth: StateFlow<Boolean> = _isCheckingHealth.asStateFlow()

    // Feedback messages (snackbars)
    private val _uiEvents = MutableSharedFlow<UiMessage>()
    val uiEvents: SharedFlow<UiMessage> = _uiEvents.asSharedFlow()

    init {
        // Try to refresh profile if token exists
        if (secureTokenManager.hasUserToken()) {
            viewModelScope.launch {
                authRepo.refreshProfile()
            }
        }

        // Initial Data Fetch
        refreshAllStoreData()
        testServerHealth()
    }

    fun emitMessage(msg: String, isError: Boolean = false) {
        viewModelScope.launch {
            _uiEvents.emit(UiMessage(msg, isError))
        }
    }

    fun refreshAllStoreData() {
        loadCategories()
        loadProducts()
    }

    fun testServerHealth() {
        viewModelScope.launch {
            _isCheckingHealth.value = true
            storeRepo.getHealth().onSuccess {
                _healthStatus.value = it
            }.onFailure {
                _healthStatus.value = HealthData(status = "offline", database = "error")
            }
            _isCheckingHealth.value = false
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            storeRepo.getCategories().onSuccess {
                _categories.value = it
            }
        }
    }

    fun selectCategory(cat: CategoryDto?) {
        _selectedCategory.value = cat
        loadProducts(categoryId = cat?.id, query = _searchQuery.value)
    }

    fun onSearchQueryChanged(q: String) {
        _searchQuery.value = q
        loadProducts(categoryId = _selectedCategory.value?.id, query = q)
    }

    fun loadProducts(categoryId: Int? = _selectedCategory.value?.id, query: String? = _searchQuery.value) {
        viewModelScope.launch {
            _isLoadingProducts.value = true
            storeRepo.getProducts(categoryId = categoryId, query = query).onSuccess { list ->
                _products.value = list
            }.onFailure {
                // If remote server returns empty or error, fallback to curated offline store products
                if (_products.value.isEmpty()) {
                    _products.value = getFallbackProducts()
                }
            }
            _isLoadingProducts.value = false
        }
    }

    fun selectProduct(product: ProductDto?) {
        _selectedProduct.value = product
    }

    // ====================================================
    // USER AUTHENTICATION ACTIONS
    // ====================================================

    fun register(
        name: String,
        email: String,
        phone: String?,
        pass: String,
        onSuccess: () -> Unit
    ) {
        if (name.isBlank() || email.isBlank() || pass.isBlank()) {
            emitMessage("يرجى ملء جميع الحقول المطلوبة", true)
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            when (val res = authRepo.register(name, email, phone, pass)) {
                is AuthResult.Success -> {
                    emitMessage(res.message ?: "تم إنشاء الحساب بنجاح!")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    emitMessage(res.message, true)
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun login(email: String, pass: String, onSuccess: () -> Unit) {
        if (email.isBlank() || pass.isBlank()) {
            emitMessage("يرجى كتابة البريد وكلمة المرور", true)
            return
        }
        viewModelScope.launch {
            _isAuthLoading.value = true
            when (val res = authRepo.login(email, pass)) {
                is AuthResult.Success -> {
                    emitMessage(res.message ?: "أهلاً بك مجدداً!")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    emitMessage(res.message, true)
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun updateProfile(name: String, phone: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            when (val res = authRepo.updateProfile(name, phone)) {
                is AuthResult.Success -> {
                    emitMessage(res.message ?: "تم تحديث البيانات بنجاح")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    emitMessage(res.message, true)
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun changePassword(oldPass: String, newPass: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            when (val res = authRepo.changePassword(oldPass, newPass)) {
                is AuthResult.Success -> {
                    emitMessage(res.message ?: "تم تغيير كلمة المرور بنجاح")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    emitMessage(res.message, true)
                }
            }
            _isAuthLoading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepo.logout()
            emitMessage("تم تسجيل الخروج بنجاح")
        }
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isAuthLoading.value = true
            when (val res = authRepo.deleteAccount()) {
                is AuthResult.Success -> {
                    emitMessage("تم حذف الحساب بنجاح")
                    onSuccess()
                }
                is AuthResult.Error -> {
                    emitMessage(res.message, true)
                }
            }
            _isAuthLoading.value = false
        }
    }

    // ====================================================
    // CART & ORDERS ACTIONS
    // ====================================================

    fun addToCart(product: ProductDto, qty: Int = 1) {
        viewModelScope.launch {
            storeRepo.addToCart(product, qty)
            emitMessage("تمت إضافة '${product.name}' إلى السلة")
        }
    }

    fun updateCartQuantity(productId: Int, delta: Int) {
        viewModelScope.launch {
            storeRepo.updateCartQuantity(productId, delta)
        }
    }

    fun removeFromCart(productId: Int) {
        viewModelScope.launch {
            storeRepo.removeFromCart(productId)
            emitMessage("تم حذف العنصر من السلة")
        }
    }

    fun checkout(shippingAddress: String, onSuccess: () -> Unit) {
        val currentItems = cartItems.value
        if (currentItems.isEmpty()) {
            emitMessage("السلة فارغة", true)
            return
        }

        viewModelScope.launch {
            _isAuthLoading.value = true
            storeRepo.createOrder(shippingAddress, currentItems).onSuccess { msg ->
                emitMessage(msg)
                onSuccess()
            }.onFailure { e ->
                emitMessage(e.message ?: "فشل في إتمام الطلب", true)
            }
            _isAuthLoading.value = false
        }
    }

    fun loadOrders() {
        viewModelScope.launch {
            _isLoadingOrders.value = true
            storeRepo.getOrders().onSuccess {
                _orders.value = it
            }.onFailure {
                // If orders endpoint returned error
            }
            _isLoadingOrders.value = false
        }
    }

    fun updateServerConfig(newUrl: String, newApiKey: String) {
        appPreferences.apiBaseUrl = newUrl
        appPreferences.apiKey = newApiKey
        emitMessage("تم حفظ إعدادات الخادم بنجاح")
        testServerHealth()
        refreshAllStoreData()
    }

    private fun getFallbackProducts(): List<ProductDto> {
        return listOf(
            ProductDto(
                id = 1,
                name = "هاتف Nova Ultra Pro 5G",
                description = "هاتف ذكي بشاشة AMOLED 120Hz، معالج رائد فائق السرعة، كاميرا 108MP وبطارية 5000mAh تدوم طويلاً مع شحن سريع 67W.",
                price = 749.0,
                originalPrice = 899.0,
                imageUrl = "https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=600",
                stock = 25,
                rating = 4.9,
                isFeatured = 1,
                categoryId = 1,
                categoryName = "الهواتف والأجهزة اللوحية"
            ),
            ProductDto(
                id = 2,
                name = "لابتوب NovaBook Pro X 16",
                description = "كمبيوتر محمول بشاشة 3K فائقة الدقة، معالج Core i9، ذاكرة 32GB RAM وقرص تخزين 1TB NVMe للأعمال والتصميم والمونتاج.",
                price = 1299.0,
                originalPrice = 1499.0,
                imageUrl = "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=600",
                stock = 15,
                rating = 4.8,
                isFeatured = 1,
                categoryId = 2,
                categoryName = "الحواسيب واللابتوب"
            ),
            ProductDto(
                id = 3,
                name = "سماعات Nova SoundFlow ANC",
                description = "سماعات رأس لاسلكية بخاصية إلغاء الضوضاء الفعال النشط، صوت محيطي عالي الدقة Hi-Res وعمر بطارية يصل إلى 40 ساعة.",
                price = 189.0,
                originalPrice = 240.0,
                imageUrl = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=600",
                stock = 40,
                rating = 4.7,
                isFeatured = 1,
                categoryId = 3,
                categoryName = "السماعات والصوتيات"
            ),
            ProductDto(
                id = 4,
                name = "ساعة ذكية Nova Watch GT 3",
                description = "شاشة AMOLED ملونة دائماً، تتبع معدل نبضات القلب ونسبة الأكسجين ومقاومة للماء حتى عمق 50 متراً مع دعم المكالمات.",
                price = 149.0,
                originalPrice = 199.0,
                imageUrl = "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=600",
                stock = 30,
                rating = 4.8,
                isFeatured = 1,
                categoryId = 4,
                categoryName = "الساعات الذكية"
            ),
            ProductDto(
                id = 5,
                name = "يد تحكم Nova Elite Wireless",
                description = "يد تحكم للألعاب مع أزرار لمسية ميكانيكية، استجابة فائقة السرعة، اهتزاز واقعي متوافقة مع الكمبيوتر والهواتف والكونسول.",
                price = 79.0,
                originalPrice = 99.0,
                imageUrl = "https://images.unsplash.com/photo-1600080972464-8e5f35f63d08?w=600",
                stock = 50,
                rating = 4.9,
                isFeatured = 1,
                categoryId = 5,
                categoryName = "الألعاب وملحقاتها"
            ),
            ProductDto(
                id = 6,
                name = "تابلت Nova Pad 11 بوصة",
                description = "شاشة عرض 2K بدعم القلم الذكي، مكبرات صوت رباعية ستيريو، مناسب للدراسة والرسم ومشاهدة الوسائط والعمل أثناء التنقل.",
                price = 349.0,
                originalPrice = 399.0,
                imageUrl = "https://images.unsplash.com/photo-1544244015-0df4b3ffc6b0?w=600",
                stock = 20,
                rating = 4.6,
                isFeatured = 0,
                categoryId = 1,
                categoryName = "الهواتف والأجهزة اللوحية"
            )
        )
    }
}

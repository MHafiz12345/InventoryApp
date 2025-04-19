# 📦 WareWise: Warehouse Management System

A modern mobile inventory management system built with **Android (Kotlin)** and powered by **Appwrite**. Designed to help businesses manage warehouses, track stock levels, control user roles, and audit inventory activities with ease.

<p align="center">
  <img src="https://github.com/user-attachments/assets/6a27c39d-4411-463f-921b-01593d93bcad" width="250" alt="WareWise Login Screen"/>
</p>

## ✨ Features

* 🔐 **Authentication & Roles** - Login system with role-based access control (Admin / Staff)
* 📊 **Dashboard Overview** - Quick stats on total items, inventory value, and low stock alerts
* 🏷️ **Item Management** - Add, edit, or remove inventory items with details like stock, price, location, and SKU
* 🏭 **Warehouse Support** - Multi-warehouse support with floor and section mapping
* 🧾 **Stock Movement Logging** - Auto-logs initial entries, stock updates, and reasons for changes
* 🛠️ **Activity Logs** - Keeps a full history of who did what and when
* 👥 **User Management (Admin Only)** - View, create, activate/deactivate, and assign roles to users
* 🔍 **Smart Filtering** - Search inventory by name or SKU. Filter by warehouse, category, and stock status
* 🔁 **Password Reset & Profile Settings** - Secure password recovery with cooldown and in-app profile controls
* 📱 **Responsive Design** - Clean, intuitive UI designed for various Android device sizes

## 📱 App Screenshots

<p align="center">
  <img src="https://github.com/user-attachments/assets/6a27c39d-4411-463f-921b-01593d93bcad" width="200" alt="Login Screen"/>
  <img src="https://github.com/user-attachments/assets/70676fee-8693-4023-909b-0ec0cc389f02" width="200" alt="Forgot Password"/>
  <img src="https://github.com/user-attachments/assets/3e7186b0-7990-48d8-b442-58c331b6fe83" width="200" alt="Dashboard"/>
</p>

<p align="center">
  <img src="https://github.com/user-attachments/assets/9bd60cb8-e6a3-45fb-8318-38a75c48dc25" width="200" alt="Add User"/>
  <img src="https://github.com/user-attachments/assets/9857bdb5-5c8d-4c12-acd5-8c88f245917a" width="200" alt="Inventory"/>
  <img src="https://github.com/user-attachments/assets/c6881033-d25f-45c2-b82d-2732b19e6e16" width="200" alt="Item Details"/>
</p>

## 🗂️ Database Structure

The application is built on a robust database schema designed for comprehensive inventory management:

<p align="center">
  <img src="https://github.com/user-attachments/assets/99236781-aba9-4a9e-b4a9-e7899b633ceb" width="700" alt="Database ERD"/>
</p>

### Key Tables/Collections

#### Warehouses
```sql
CREATE TABLE warehouses (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  name varchar(255) NOT NULL,
  location varchar(255) NOT NULL,
  address varchar(255),
  floors json,
  sections json
);
```

#### Categories
```sql
CREATE TABLE categories (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  name varchar(255) NOT NULL,
  description text
);
```

#### Inventory Items
```sql
CREATE TABLE inventory_items (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  name varchar(255) NOT NULL,
  description text,
  price float,
  current_stock int NOT NULL DEFAULT 0,
  min_stock int NOT NULL DEFAULT 0,
  status varchar(50),
  sku varchar(50),
  floor varchar(50),
  section varchar(50),
  category_id bigint REFERENCES categories(id),
  warehouse_id bigint REFERENCES warehouses(id)
);
```

#### Users
```sql
CREATE TABLE users (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  email varchar(255) NOT NULL,
  role varchar(50),
  department varchar(255),
  is_active tinyint(1) DEFAULT 1,
  last_login timestamp,
  permissions json
);
```

#### Activity Logs
```sql
CREATE TABLE activity_logs (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  action_type varchar(50) NOT NULL,
  old_value text,
  new_value text,
  details text,
  user_id bigint REFERENCES users(id),
  inventory_item_id bigint REFERENCES inventory_items(id)
);
```

#### Stock Movements
```sql
CREATE TABLE stock_movements (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  quantity int NOT NULL,
  type varchar(50) NOT NULL,
  reason text,
  inventory_item_id bigint REFERENCES inventory_items(id),
  warehouse_id bigint REFERENCES warehouses(id),
  user_id bigint REFERENCES users(id)
);
```

## 🧩 Tech Stack

### Frontend
* **Language**: Kotlin
* **UI**: XML layouts with Material Design components
* **Architecture**: MVVM-inspired pattern
* **Navigation**: Android Navigation Component
* **Data Binding**: View binding for cleaner UI interactions

### Backend (Appwrite)
* **Authentication**: Appwrite Auth API
* **Database**: Appwrite Database
* **Storage**: Appwrite Storage for any product images
* **User Management**: Appwrite Account API
* **Security**: JWT token-based authentication and role-based access control

### Libraries & Dependencies
* **Networking**: Retrofit, OkHttp
* **Async Programming**: Kotlin Coroutines, Flow
* **UI Components**: Material Components, RecyclerView, CardView
* **Image Loading**: Glide or Coil
* **Charts**: MPAndroidChart for dashboard statistics

## 🚀 Getting Started

### Prerequisites
* Android Studio Arctic Fox or later
* Kotlin 1.5+
* Appwrite server instance
* JDK 11+
* Android device or emulator running API level 21+

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/yourusername/warewise.git
   cd warewise
   ```

2. **Set up Appwrite**
   - Create an Appwrite project
   - Set up collections for all entities (users, warehouses, inventory_items, etc.)
   - Configure authentication methods
   - Create API keys with appropriate permissions

3. **Configure the application**
   - Open the project in Android Studio
   - Update `app/src/main/res/values/appwrite_config.xml` with your Appwrite endpoint, project ID, and collection IDs

4. **Build and Run**
   ```bash
   ./gradlew assembleDebug
   ```
   Or use the Run button in Android Studio

### Default Credentials

For testing purposes, the application includes a default admin account:
- **Email**: admin@warewise.com
- **Password**: Admin123!

⚠️ **Important**: Change these credentials immediately in a production environment.

## 🛡️ Security Features

- Password hashing handled by Appwrite
- JWT token-based session management
- Role-based permission system (Admin vs Staff)
- Password recovery with email verification
- Activity logging for audit trails
- Session timeout for security
- Input validation to prevent SQL injection

## 📱 App Workflow

### User Authentication
1. Users log in with email and password
2. Optional "Remember Me" feature for convenience
3. Forgot password workflow with email verification
4. Role-based redirection after login

### Dashboard (Home)
1. Overview of inventory statistics
2. Low stock alerts and their locations
3. Quick-action buttons for common tasks
4. Total inventory value calculation

### Inventory Management
1. List of all items with filtering options
2. Add new items with category, warehouse, and stock information
3. Edit existing items and adjust stock levels
4. View detailed stock movement history per item
5. Scan barcodes (SKU) to quickly access items

### User Management (Admin only)
1. View all system users
2. Add new users with specific roles
3. Deactivate/activate existing users
4. Reset user passwords if needed

### Profile Section
1. View and edit personal information
2. Change password
3. View activity history

## 🔄 Synchronization

WareWise implements real-time synchronization between devices:
- Changes made by one user are immediately visible to others
- Offline support with local caching and sync when back online
- Conflict resolution prioritizes server data with notification

## 📈 Future Enhancements

- **Barcode Scanner Integration**: Native barcode and QR code scanning
- **Export Functionality**: Data export to CSV/PDF
- **Advanced Analytics**: Trend analysis and predictive stock recommendations
- **Multi-language Support**: Internationalization for global use
- **Supplier Management**: Add supplier tracking and order management
- **Custom Notifications**: Email/push alerts for critical stock events
- **Dark Mode**: Alternative UI theme

## 📌 Notes

* Default login supports Remember Me functionality
* SKUs are auto-generated but checked for uniqueness
* All database operations leverage Appwrite's security features
* Stock movements and activity logs are created automatically on actions
* Minimum stock levels trigger visual alerts on dashboard

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 📥 Contributions

While this app was developed for internal usage or academic demonstration, contributions are welcome. Suggestions, issues, and improvements can be submitted via GitHub Issues or Pull Requests.

﻿-- Kiểm tra và tạo Database nếu chưa tồn tại
BEGIN
    CREATE DATABASE productShopessss;
END;
GO
USE productShopessss;
GO

-- Tạo bảng Accounts (Người dùng)
CREATE TABLE Accounts
(
    id NVARCHAR(100) NOT NULL PRIMARY KEY,
    phone NVARCHAR(15) UNIQUE,
    fullname NVARCHAR(100),
    image NVARCHAR(MAX),
    email NVARCHAR(350) NOT NULL UNIQUE,
    address NVARCHAR(MAX),
    password NVARCHAR(255) NOT NULL,
    birthday DATE,
    gender CHAR(1) CHECK (gender IN ('M', 'F', 'O')),
    status INT DEFAULT 1 NOT NULL,
    verified BIT DEFAULT 0 NOT NULL,
    created_date DATETIME DEFAULT GETDATE(),
    points INT DEFAULT 0 NOT NULL
);
GO

-- Tạo bảng lock_reasons (Lý do khóa tài khoản)
CREATE TABLE dbo.lock_reasons
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    account_id NVARCHAR(100) NOT NULL,
    reason NVARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    FOREIGN KEY (account_id) REFERENCES dbo.Accounts(id)
);

-- Tạo bảng Roles (Vai trò người dùng)
CREATE TABLE Roles
(
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE,
    description NVARCHAR(MAX)
);
GO

-- Tạo bảng Accounts_Roles (Quan hệ tài khoản và vai trò)
CREATE TABLE Accounts_Roles
(
    account_id NVARCHAR(100) NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (account_id, role_id),
    FOREIGN KEY (account_id) REFERENCES Accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES Roles(id) ON DELETE CASCADE
);
GO

-- Tạo bảng Verification (Mã xác thực tài khoản)
CREATE TABLE Verification
(
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    account_id NVARCHAR(100) NOT NULL,
    code NVARCHAR(50) NOT NULL,
    created_at DATETIME DEFAULT GETDATE(),
    expires_at DATETIME,
    active BIT DEFAULT 1,
    FOREIGN KEY (account_id) REFERENCES Accounts(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_verification_account_id ON Verification(account_id);

-- Tạo bảng Categories (Danh mục sản phẩm)
CREATE TABLE Categories
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(50) NOT NULL UNIQUE,
    description NVARCHAR(200)
);
GO

-- Tạo bảng Products (Sản phẩm)
CREATE TABLE Products
(
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name NVARCHAR(MAX) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    price DECIMAL(18,2) NOT NULL CHECK (price >= 0),
    description NVARCHAR(MAX),
    status BIT NOT NULL DEFAULT 1,
    category_id INT NOT NULL,
    FOREIGN KEY (category_id) REFERENCES Categories(id) ON DELETE CASCADE
)
GO
CREATE INDEX idx_product_id ON Product_Images(product_id);
CREATE INDEX idx_product_sizes ON Product_Sizes(product_id);

-- Tạo bảng Suppliers (Nhà cung cấp)
CREATE TABLE Suppliers
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL UNIQUE,
    phone_number NVARCHAR(10) NOT NULL,
    email NVARCHAR(100),
    address NVARCHAR(MAX)
);
GO

-- Tạo bảng Brands (Thương hiệu)
CREATE TABLE Brands
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(200) NOT NULL UNIQUE,
    phone_number NVARCHAR(10) NOT NULL,
    email NVARCHAR(100),
    address NVARCHAR(MAX)
);
GO

-- Tạo bảng Stock_Receipts (Nhập hàng)
GO
CREATE TABLE Stock_Receipts
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    supplier_id INT NOT NULL,
    brand_id INT NOT NULL,
    order_date DATE NOT NULL DEFAULT GETDATE(),
    FOREIGN KEY (supplier_id) REFERENCES Suppliers(id) ON DELETE CASCADE,
    FOREIGN KEY (brand_id) REFERENCES Brands(id) ON DELETE CASCADE,
);
GO
-- Tạo bảng Receipt_Products (Chi tiết nhập hàng)
CREATE TABLE Receipt_Products
(
    receipt_id INT,
    product_id INT,
    quantity INT NOT NULL CHECK (quantity >= 0),
    price DECIMAL(15, 2) NOT NULL CHECK (price >= 0),
    total_amount AS (quantity * price) PERSISTED,
    PRIMARY KEY (receipt_id, product_id),
    FOREIGN KEY (receipt_id) REFERENCES Stock_Receipts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    CONSTRAINT CHK_TotalAmount CHECK (total_amount = quantity * price)
);

CREATE INDEX idx_products_category_id ON Products(category_id);

-- Tạo bảng Product_Images (Hình ảnh sản phẩm)
CREATE TABLE Product_Images
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    product_id INT NOT NULL,
    image_url NVARCHAR(MAX) NOT NULL,
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
GO

-- Tạo bảng Sizes (Kích thước sản phẩm)
CREATE TABLE Sizes
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(255) NOT NULL UNIQUE,
    description NVARCHAR(100) NULL
);
GO

-- Tạo bảng Product_Sizes (Quan hệ sản phẩm và kích thước)
CREATE TABLE Product_Sizes
(
    id INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    product_id INT NOT NULL,
    size_id INT NOT NULL,
    quantity INT NOT NULL DEFAULT 0 CHECK (quantity >= 0),
    price DECIMAL(18,2) NOT NULL DEFAULT 0.00 CHECK (price >= 0),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    FOREIGN KEY (size_id) REFERENCES Sizes(id) ON DELETE CASCADE
);
GO
CREATE INDEX idx_product_sizes_product_id ON Product_Sizes(product_id);

-- Tạo bảng Comments (Bình luận sản phẩm)
CREATE TABLE Comments
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    content NVARCHAR(MAX),
    like_count INT DEFAULT 0,
    created_at DATETIME DEFAULT GETDATE(),
    user_id NVARCHAR(100) NOT NULL,
    product_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES Accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE
);
GO

-- Tạo bảng cancel_reasons (Lý do hủy)
CREATE TABLE cancel_reasons
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    reason NVARCHAR(255) NOT NULL UNIQUE
);
GO

-- Tạo bảng Invoices (Hóa đơn)
CREATE TABLE Invoices
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    order_date DATETIME NOT NULL DEFAULT GETDATE(),
    address NVARCHAR(200),
    status NVARCHAR(50) NOT NULL DEFAULT 'PENDING',
    note NVARCHAR(200),
    total_amount DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    user_id NVARCHAR(100) NOT NULL,
    cancel_reason_id INT,
    FOREIGN KEY (user_id) REFERENCES Accounts(id) ON DELETE CASCADE,
    FOREIGN KEY (cancel_reason_id) REFERENCES cancel_reasons(id) ON DELETE SET NULL,
    CONSTRAINT CHK_Invoice_Status CHECK (status IN ('PENDING', 'SHIPPING', 'DELIVERED', 'CANCELLED'))
);
GO



-- Tạo bảng Detailed_Invoices (Chi tiết hóa đơn)
CREATE TABLE Detailed_Invoices
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    invoice_id INT NOT NULL,
    product_id INT NOT NULL,
    size_id INT NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0),
    unit_price DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    payment_method NVARCHAR(200) NOT NULL,
    FOREIGN KEY (invoice_id) REFERENCES Invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    FOREIGN KEY (size_id) REFERENCES Sizes(id) ON DELETE CASCADE
);

CREATE INDEX IX_DetailedInvoices_InvoiceId ON Detailed_Invoices(invoice_id);
CREATE INDEX IX_DetailedInvoices_ProductSize ON Detailed_Invoices(product_id, size_id);
CREATE INDEX IX_Invoices_Status ON Invoices(status);
CREATE INDEX IX_Invoices_UserId ON Invoices(user_id);
GO

-- Tạo bảng Product_Attributes (Đặc điểm sản phẩm)
CREATE TABLE Product_Attributes
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    name NVARCHAR(MAX) NOT NULL
);
GO

-- Tạo bảng Product_Attribute_Mappings (Quan hệ sản phẩm và đặc điểm)
CREATE TABLE Product_Attribute_Mappings
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    product_id INT NOT NULL,
    attribute_id INT NOT NULL,
    UNIQUE(product_id, attribute_id),
    FOREIGN KEY (product_id) REFERENCES Products(id) ON DELETE CASCADE,
    FOREIGN KEY (attribute_id) REFERENCES Product_Attributes(id) ON DELETE CASCADE
);
GO

-- Tạo bảng User_Histories (Lịch sử người dùng)
CREATE TABLE UserHistory
(
    id_history INT IDENTITY(1,1) PRIMARY KEY,
    note NVARCHAR(1000),
    history_datetime DATETIME NOT NULL DEFAULT GETDATE(),
    user_id NVARCHAR(100) NOT NULL,
    actionType NVARCHAR(50),
    ipAddress NVARCHAR(45),
    deviceInfo NVARCHAR(200),
    status INT DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES Accounts(id) ON DELETE CASCADE
);
GO

-- Tạo bảng RefreshToken (Token làm mới)
CREATE TABLE RefreshToken
(
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id NVARCHAR(100) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    CONSTRAINT FK_RefreshToken_Account FOREIGN KEY (user_id) 
        REFERENCES Accounts(id) ON DELETE CASCADE
);
GO

-- Tạo bảng Feedback (Góp ý khách hàng)
CREATE TABLE Feedback
(
    id INT IDENTITY(1,1) PRIMARY KEY,
    email NVARCHAR(350) NOT NULL,
    message NVARCHAR(MAX) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT GETDATE(),
    status INT NOT NULL DEFAULT 0 -- 0: Chưa xử lý, 1: Đang xử lý, 2: Đã xử lý
);
GO

-- Tạo index cho bảng Feedback
CREATE INDEX IX_Feedback_Email ON Feedback(email);
CREATE INDEX IX_Feedback_Status ON Feedback(status);
CREATE INDEX IX_Feedback_CreatedAt ON Feedback(created_at);
GO
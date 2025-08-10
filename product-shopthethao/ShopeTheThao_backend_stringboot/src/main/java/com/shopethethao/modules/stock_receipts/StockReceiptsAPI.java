package com.shopethethao.modules.stock_receipts;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.shopethethao.dto.ReceiptProductDTO;
import com.shopethethao.dto.ReceiptProductRequestDTO;
import com.shopethethao.dto.ResponseDTO;
import com.shopethethao.dto.StockReceiptDTO;
import com.shopethethao.dto.StockReceiptRequestDTO;
import com.shopethethao.modules.brands.Brand;
import com.shopethethao.modules.brands.BrandDAO;
import com.shopethethao.modules.products.Product;
import com.shopethethao.modules.products.ProductsDAO;
import com.shopethethao.modules.receipt_Products.ReceiptProduct;
import com.shopethethao.modules.receipt_Products.ReceiptProductDAO;
import com.shopethethao.modules.receipt_Products.ReceiptProductPK;
import com.shopethethao.modules.suppliers.Supplier;
import com.shopethethao.modules.suppliers.SupplierDAO;
import com.shopethethao.modules.userHistory.UserActionType;
import com.shopethethao.service.UserHistoryService;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/stockReceipts")
public class StockReceiptsAPI {

    private static final Logger logger = LoggerFactory.getLogger(StockReceiptsAPI.class);

    @Autowired
    private StockReceiptsDAO stockReceiptsDAO;

    @Autowired
    private ReceiptProductDAO receiptProductDAO;

    @Autowired
    private SupplierDAO supplierDAO;

    @Autowired
    private BrandDAO brandDAO;

    @Autowired
    private ProductsDAO productsDAO;

    @Autowired
    private UserHistoryService userHistoryService;

    @GetMapping("/get/all")
    public ResponseEntity<List<StockReceipt>> findAll() {
        List<StockReceipt> stockReceipts = stockReceiptsDAO.findAll();
        return ResponseEntity.ok(stockReceipts);
    }

    @GetMapping
    public ResponseEntity<?> findAll(
            @RequestParam("page") Optional<Integer> pageNo,
            @RequestParam("limit") Optional<Integer> limit,
            @RequestParam("search") Optional<String> search) {
        try {
            if (pageNo.isPresent() && pageNo.get() == 0) {
                return handleError("Trang không tồn tại", HttpStatus.NOT_FOUND);
            }
            
            Pageable pageable = PageRequest.of(
                pageNo.orElse(1) - 1, 
                limit.orElse(10), 
                Sort.by(Sort.Order.desc("id"))
            );
            
            Page<StockReceipt> pageResult = search
                .filter(s -> !s.trim().isEmpty())
                .map(s -> stockReceiptsDAO.searchStockReceipts(s.trim(), pageable))
                .orElseGet(() -> stockReceiptsDAO.findAll(pageable));

            ResponseDTO<StockReceiptDTO> responseDTO = new ResponseDTO<>();
            responseDTO.setData(pageResult.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
            responseDTO.setTotalItems(pageResult.getTotalElements());
            responseDTO.setTotalPages(pageResult.getTotalPages());

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            logger.error("Error fetching stock receipts", e);
            return handleError("Lỗi máy chủ, vui lòng thử lại sau!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    

    // Phương thức chuyển đổi StockReceipt thành StockReceiptDTO
    private StockReceiptDTO convertToDTO(StockReceipt stockReceipt) {
        StockReceiptDTO dto = new StockReceiptDTO();
        dto.setId(stockReceipt.getId());
        dto.setSupplierName(stockReceipt.getSupplier().getName());
        dto.setSupplierId(stockReceipt.getSupplier().getId());
        dto.setBrandId(stockReceipt.getBrand().getId());
        dto.setBrandName(stockReceipt.getBrand().getName());
        dto.setOrderDate(stockReceipt.getOrder_date());

        // Chuyển đổi ReceiptProduct thành ReceiptProductDTO
        List<ReceiptProductDTO> receiptProductDTOs = stockReceipt.getReceiptProducts().stream()
                .map(this::convertReceiptProductToDTO)
                .collect(Collectors.toList());

        dto.setReceiptProducts(receiptProductDTOs);
        return dto;
    }

    // Phương thức chuyển đổi ReceiptProduct thành ReceiptProductDTO
    private ReceiptProductDTO convertReceiptProductToDTO(ReceiptProduct receiptProduct) {
        ReceiptProductDTO dto = new ReceiptProductDTO();
        dto.setProductId(receiptProduct.getProduct().getId());
        dto.setProductName(receiptProduct.getProduct().getName());
        dto.setQuantity(receiptProduct.getQuantity());
        dto.setPrice(receiptProduct.getPrice());
        dto.setTotalAmount(receiptProduct.getTotal_amount());
        return dto;
    }

    // ✅ Tạo phiếu nhập kho
    @Transactional
    @PostMapping
    public ResponseEntity<?> createStockReceipt(@RequestBody StockReceiptRequestDTO request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            // Validate required fields
            if (request.getSupplierId() == null || request.getBrandId() == null ||
                    request.getOrderDate() == null || request.getReceiptProducts() == null ||
                    request.getReceiptProducts().isEmpty()) {
                return handleError("Thông tin supplier, brand, ngày đặt và danh sách sản phẩm là bắt buộc.",
                        HttpStatus.BAD_REQUEST);
            }

            // Validate supplier and brand
            Supplier supplier = supplierDAO.getReferenceById(request.getSupplierId());
            Brand brand = brandDAO.getReferenceById(request.getBrandId());

            // Create and save StockReceipt
            StockReceipt stockReceipt = new StockReceipt();
            stockReceipt.setSupplier(supplier);
            stockReceipt.setBrand(brand);
            stockReceipt.setOrder_date(request.getOrderDate());
            StockReceipt savedStockReceipt = stockReceiptsDAO.save(stockReceipt);

            // Build detailed log message
            StringBuilder logMessage = new StringBuilder();
            logMessage.append(String.format("""
                    ADMIN: %s đã tạo phiếu nhập kho mới
                    Chi tiết:
                    - Mã phiếu: #%d
                    - Nhà cung cấp: %s
                    - Thương hiệu: %s
                    - Ngày đặt: %s

                    Danh sách sản phẩm:
                    """,
                    authentication.getName(),
                    savedStockReceipt.getId(),
                    supplier.getName(),
                    brand.getName(),
                    request.getOrderDate()));

            // Log each product detail using the helper method
            request.getReceiptProducts().forEach(product -> {
                Product productInfo = productsDAO.getReferenceById(product.getProductId());
                BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
                logMessage.append(formatProductDetail(
                        productInfo.getName(),
                        product.getQuantity(),
                        product.getPrice(),
                        totalAmount));
            });

            userHistoryService.logUserAction(
                    authentication.getName(),
                    UserActionType.CREATE_STOCK_RECEIPT,
                    logMessage.toString(),
                    getClientIp(httpRequest),
                    getClientInfo(httpRequest));

            // Process ReceiptProducts
            Set<Integer> processedProductIds = new HashSet<>();
            List<ReceiptProduct> receiptProducts = new ArrayList<>();

            for (ReceiptProductRequestDTO productRequest : request.getReceiptProducts()) {
                // Validate product request
                if (productRequest.getProductId() == null || productRequest.getQuantity() == null ||
                        productRequest.getPrice() == null || productRequest.getQuantity() <= 0 ||
                        productRequest.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Thông tin sản phẩm không hợp lệ");
                }

                // Check for duplicate products
                if (!processedProductIds.add(productRequest.getProductId())) {
                    throw new IllegalArgumentException("Sản phẩm trùng lặp trong danh sách");
                }

                // Get and validate product
                Product product = productsDAO.findById(productRequest.getProductId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Sản phẩm không tồn tại với ID: " + productRequest.getProductId()));

                // Create ReceiptProduct
                ReceiptProduct receiptProduct = new ReceiptProduct();
                ReceiptProductPK pk = new ReceiptProductPK();
                pk.setReceiptId(savedStockReceipt.getId());
                pk.setProductId(productRequest.getProductId());

                receiptProduct.setId(pk);
                receiptProduct.setStockReceipt(savedStockReceipt);
                receiptProduct.setProduct(product);
                receiptProduct.setQuantity(productRequest.getQuantity());
                receiptProduct.setPrice(productRequest.getPrice());
                receiptProduct.setTotal_amount(
                        productRequest.getPrice().multiply(BigDecimal.valueOf(productRequest.getQuantity())));

                receiptProducts.add(receiptProduct);
            }

            // Save all receipt products
            receiptProductDAO.saveAll(receiptProducts);
            savedStockReceipt.setReceiptProducts(receiptProducts);

            // Return success response with created receipt
            return ResponseEntity.ok(convertToDTO(savedStockReceipt));

        } catch (IllegalArgumentException e) {
            return handleError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("Error creating stock receipt", e);
            return handleError("Lỗi khi tạo phiếu nhập kho: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Transactional
    @PutMapping("/{id}")
    public ResponseEntity<?> updateStockReceipt(@PathVariable("id") Integer id,
            @Valid @RequestBody StockReceiptRequestDTO request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            StockReceipt existingStockReceipt = stockReceiptsDAO.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Phiếu nhập không tồn tại với ID: " + id));

            Supplier supplier = supplierDAO.findById(request.getSupplierId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy nhà cung cấp với ID: " + request.getSupplierId()));
            Brand brand = brandDAO.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Không tìm thấy thương hiệu với ID: " + request.getBrandId()));

            existingStockReceipt.setSupplier(supplier);
            existingStockReceipt.setBrand(brand);
            existingStockReceipt.setOrder_date(request.getOrderDate());

            // Track changes
            List<String> changes = new ArrayList<>();

            // Check supplier changes
            if (!existingStockReceipt.getSupplier().getId().equals(request.getSupplierId())) {
                changes.add(String.format("- Nhà cung cấp:%n  + Cũ: '%s'%n  + Mới: '%s'",
                        existingStockReceipt.getSupplier().getName(),
                        supplierDAO.findById(request.getSupplierId()).get().getName()));
            }

            // Check brand changes
            if (!existingStockReceipt.getBrand().getId().equals(request.getBrandId())) {
                changes.add(String.format("- Thương hiệu:%n  + Cũ: '%s'%n  + Mới: '%s'",
                        existingStockReceipt.getBrand().getName(),
                        brandDAO.findById(request.getBrandId()).get().getName()));
            }

            // Check date changes
            if (!existingStockReceipt.getOrder_date().equals(request.getOrderDate())) {
                changes.add(String.format("- Ngày đặt:%n  + Cũ: '%s'%n  + Mới: '%s'",
                        existingStockReceipt.getOrder_date(),
                        request.getOrderDate()));
            }

            // Clear existing receipt products
            receiptProductDAO.deleteByStockReceiptId(existingStockReceipt.getId());
            existingStockReceipt.getReceiptProducts().clear();

            // Add new receipt products
            for (ReceiptProductRequestDTO productRequest : request.getReceiptProducts()) {
                Product product = productsDAO.findById(productRequest.getProductId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Sản phẩm không tồn tại với ID: " + productRequest.getProductId()));

                ReceiptProduct receiptProduct = new ReceiptProduct();

                // Set the composite key
                ReceiptProductPK productPK = new ReceiptProductPK();
                productPK.setReceiptId(existingStockReceipt.getId());
                productPK.setProductId(productRequest.getProductId());
                receiptProduct.setId(productPK);

                // Set other properties
                receiptProduct.setStockReceipt(existingStockReceipt);
                receiptProduct.setProduct(product);
                receiptProduct.setQuantity(productRequest.getQuantity());
                receiptProduct.setPrice(productRequest.getPrice());
                receiptProduct.setTotal_amount(
                        receiptProduct.getPrice().multiply(BigDecimal.valueOf(receiptProduct.getQuantity())));

                // Save the new receipt product
                receiptProduct = receiptProductDAO.save(receiptProduct);
                existingStockReceipt.getReceiptProducts().add(receiptProduct);
            }

            // Save the updated stock receipt
            stockReceiptsDAO.save(existingStockReceipt);

            // Create detailed change log
            StringBuilder logMessage = new StringBuilder();
            logMessage.append(String.format("""
                    ADMIN: %s đã cập nhật phiếu nhập kho #%d

                    Thay đổi thông tin cơ bản:
                    %s

                    Chi tiết sản phẩm mới:
                    """,
                    authentication.getName(),
                    id,
                    String.join("\n", changes)));

            // Log new product details using the helper method
            request.getReceiptProducts().forEach(product -> {
                Product productInfo = productsDAO.getReferenceById(product.getProductId());
                BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(product.getQuantity()));
                logMessage.append(formatProductDetail(
                        productInfo.getName(),
                        product.getQuantity(),
                        product.getPrice(),
                        totalAmount));
            });

            userHistoryService.logUserAction(
                    authentication.getName(),
                    UserActionType.UPDATE_STOCK_RECEIPT,
                    logMessage.toString(),
                    getClientIp(httpRequest),
                    getClientInfo(httpRequest));

            return ResponseEntity.ok(existingStockReceipt);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatusCode());
        } catch (Exception e) {
            logger.error("Error updating stock receipt", e);
            return handleError("Lỗi khi cập nhật phiếu nhập kho: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ✅ Xóa phiếu nhập kho
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStockReceipt(@PathVariable("id") Integer id,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        try {
            Optional<StockReceipt> existingReceipt = stockReceiptsDAO.findById(id);
            if (existingReceipt.isPresent()) {
                StockReceipt receipt = existingReceipt.get();

                // Create detailed log message before deletion
                StringBuilder logMessage = new StringBuilder();
                logMessage.append(String.format("""
                        ADMIN: %s đã xóa phiếu nhập kho
                        Chi tiết phiếu đã xóa:
                        - Mã phiếu: #%d
                        - Nhà cung cấp: %s
                        - Thương hiệu: %s
                        - Ngày đặt: %s

                        Danh sách sản phẩm đã xóa:
                        """,
                        authentication.getName(),
                        receipt.getId(),
                        receipt.getSupplier().getName(),
                        receipt.getBrand().getName(),
                        receipt.getOrder_date()));

                // Log deleted products using the helper method
                receipt.getReceiptProducts().forEach(product -> {
                    logMessage.append(formatProductDetail(
                            product.getProduct().getName(),
                            product.getQuantity(),
                            product.getPrice(),
                            product.getTotal_amount()));
                });

                // Perform deletion
                stockReceiptsDAO.deleteById(id);

                userHistoryService.logUserAction(
                        authentication.getName(),
                        UserActionType.DELETE_STOCK_RECEIPT,
                        logMessage.toString(),
                        getClientIp(httpRequest),
                        getClientInfo(httpRequest));

                return ResponseEntity.ok("Xóa phiếu nhập thành công!");
            } else {
                return handleError("Phiếu nhập không tồn tại!", HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            logger.error("Error deleting stock receipt", e);
            return handleError("Lỗi khi xóa phiếu nhập kho: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static final String PRODUCT_DETAIL_FORMAT = """
            - Sản phẩm: %s
              Số lượng: %d
              Đơn giá: %,.0f
              Thành tiền: %,.0f
            """;

    // Helper method to format product details
    private String formatProductDetail(String productName, int quantity, BigDecimal price, BigDecimal totalAmount) {
        return String.format(PRODUCT_DETAIL_FORMAT,
                productName,
                quantity,
                price.doubleValue(),
                totalAmount.doubleValue());
    }

    // Helper method to handle errors uniformly
    private ResponseEntity<String> handleError(String message, HttpStatus status) {
        logger.error(message);
        return new ResponseEntity<>(message, status);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    private String getClientInfo(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}

package com.shopethethao.modules.invoices;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopethethao.dto.DetailedInvoicesDTO;
import com.shopethethao.dto.InvoiceAllDTO;
import com.shopethethao.dto.InvoiceListDTO;
import com.shopethethao.dto.ResponseDTO;
import com.shopethethao.dto.SizeDTO;
import com.shopethethao.dto.UpdateStatusRequest;
import com.shopethethao.modules.cancelReason.CancelReason;
import com.shopethethao.modules.cancelReason.CancelReasonDAO;
import com.shopethethao.modules.detailed_invoices.DetailedInvoicesDAO;
import com.shopethethao.modules.product_Images.ProductImages;

@RestController
@RequestMapping("/api/invoice")
public class InvoiceAPI {

    @Autowired
    private InvoiceDAO invoiceDAO;

    @Autowired
    private CancelReasonDAO cancelReasonDAO;

    @Autowired
    private DetailedInvoicesDAO detailedInvoicesDAO;

    @GetMapping("/get/all")
    public ResponseEntity<List<Invoice>> findAll() {
        List<Invoice> detailedInvoices = invoiceDAO.findAll();
        return ResponseEntity.ok(detailedInvoices);
    }

    @GetMapping
    public ResponseEntity<?> findAll(@RequestParam("page") Optional<Integer> pageNo,
            @RequestParam("limit") Optional<Integer> limit) {
        try {
            if (pageNo.isPresent() && pageNo.get() == 0) {
                return new ResponseEntity<>("Không tìm thấy trang", HttpStatus.NOT_FOUND);
            }

            Sort sort = Sort.by(Sort.Order.desc("id"));
            Pageable pageable = PageRequest.of(pageNo.orElse(1) - 1, limit.orElse(10), sort);
            Page<Invoice> page = invoiceDAO.findAll(pageable);

            ResponseDTO<Invoice> responseDTO = new ResponseDTO<>();
            responseDTO.setData(page.getContent());
            responseDTO.setTotalItems(page.getTotalElements());
            responseDTO.setTotalPages(page.getTotalPages());

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return new ResponseEntity<>("Lỗi máy chủ, vui lòng thử lại sau!", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // @GetMapping("/get/all")
    // public ResponseEntity<?> findAll() {
    // try {
    // // Lấy tất cả hóa đơn từ database
    // List<InvoiceAllDTO> invoices = invoiceDAO.findAll().stream()
    // .map(invoice -> {
    // InvoiceAllDTO dto = new InvoiceAllDTO();
    // dto.setId(invoice.getId());
    // dto.setOrderDate(invoice.getOrderDate());
    // dto.setAddress(invoice.getAddress());
    // dto.setStatus(invoice.getStatus().toString());
    // dto.setNote(invoice.getNote());
    // dto.setTotalAmount(invoice.getTotalAmount());
    // dto.setUserId(invoice.getUser().getId());

    // // Lấy lý do hủy nếu có
    // dto.setCancelReason(
    // invoice.getCancelReason() != null ? invoice.getCancelReason().getReason() :
    // null);

    // // Lấy chi tiết hóa đơn cho mỗi hóa đơn
    // List<DetailedInvoicesDTO> detailedInvoicesDTOs =
    // detailedInvoicesDAO.findAll().stream()
    // .filter(detailedInvoice ->
    // detailedInvoice.getInvoice().getId().equals(invoice.getId()))
    // .map(detailedInvoice -> {
    // DetailedInvoicesDTO detailedDTO = new DetailedInvoicesDTO();
    // detailedDTO.setId(detailedInvoice.getId());
    // detailedDTO.setInvoiceId(detailedInvoice.getInvoice().getId());
    // detailedDTO.setProductId(detailedInvoice.getProduct().getId());
    // detailedDTO.setProductName(detailedInvoice.getProduct().getName());
    // detailedDTO.setQuantity(detailedInvoice.getQuantity());
    // detailedDTO.setUnitPrice(detailedInvoice.getUnitPrice());
    // detailedDTO.setPaymentMethod(detailedInvoice.getPaymentMethod());
    // return detailedDTO;
    // })
    // .collect(Collectors.toList());

    // dto.setDetailedInvoices(detailedInvoicesDTOs); // Set chi tiết hóa đơn vào
    // DTO

    // return dto;
    // })
    // .collect(Collectors.toList()); // Thu thập kết quả thành danh sách

    // return ResponseEntity.ok(invoices);
    // } catch (Exception e) {
    // return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    // .body("Lỗi khi lấy danh sách hóa đơn: " + e.getMessage());
    // }
    // }

    @GetMapping("/get/{id}")
    public ResponseEntity<?> findById(@PathVariable Integer id) {
        try {
            // Find the invoice by its ID
            Invoice invoice = invoiceDAO.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với ID: " + id));

            // Map the Invoice to InvoiceAllDTO
            InvoiceAllDTO dto = new InvoiceAllDTO();
            dto.setId(invoice.getId());
            dto.setOrderDate(invoice.getOrderDate());
            dto.setAddress(invoice.getAddress());
            dto.setStatus(invoice.getStatus().toString());
            dto.setNote(invoice.getNote());
            dto.setTotalAmount(invoice.getTotalAmount());
            dto.setUserId(invoice.getUser().getId());
            dto.setFullnames(invoice.getUser().getFullname());

            // Set the cancel reason if available
            dto.setCancelReason(invoice.getCancelReason() != null ? invoice.getCancelReason().getReason() : null);

            // Get the detailed invoices for this particular invoice
            List<DetailedInvoicesDTO> detailedInvoicesDTOs = detailedInvoicesDAO.findAll().stream()
                    .filter(detailedInvoice -> detailedInvoice.getInvoice().getId().equals(invoice.getId()))
                    .map(detailedInvoice -> {
                        DetailedInvoicesDTO detailedDTO = new DetailedInvoicesDTO();
                        detailedDTO.setId(detailedInvoice.getId());
                        detailedDTO.setInvoiceId(detailedInvoice.getInvoice().getId());
                        detailedDTO.setProductId(detailedInvoice.getProduct().getId());
                        detailedDTO.setProductName(detailedInvoice.getProduct().getName());
                        detailedDTO.setQuantity(detailedInvoice.getQuantity());
                        detailedDTO.setUnitPrice(detailedInvoice.getUnitPrice());
                        detailedDTO.setPaymentMethod(detailedInvoice.getPaymentMethod());

                        // Add product images
                        List<String> imageUrls = detailedInvoice.getProduct().getImages()
                                .stream()
                                .map(ProductImages::getImageUrl)
                                .collect(Collectors.toList());
                        detailedDTO.setProductImages(imageUrls);
                        List<SizeDTO> sizeDTOs = detailedInvoice.getProduct().getSizes()
                                .stream()
                                .map(productSize -> {
                                    SizeDTO sizeDTO = new SizeDTO();
                                    sizeDTO.setId(productSize.getSize().getId());
                                    sizeDTO.setName(productSize.getSize().getName());
                                    sizeDTO.setQuantity(productSize.getQuantity());
                                    sizeDTO.setPrice(productSize.getPrice());
                                    return sizeDTO;
                                })
                                .collect(Collectors.toList());
                        detailedDTO.setProductSizes(sizeDTOs);

                        detailedInvoice.getProduct().getSizes()
                                .stream()
                                .filter(productSize -> productSize.getPrice()
                                        .equals(detailedInvoice.getUnitPrice().intValue()))
                                .findFirst()
                                .ifPresent(productSize -> detailedDTO.setSelectedSize(productSize.getSize().getName()));
                        return detailedDTO;
                    })
                    .collect(Collectors.toList());

            dto.setDetailedInvoices(detailedInvoicesDTOs); // Set the detailed invoices into DTO

            return ResponseEntity.ok(dto); // Return the invoice details
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy chi tiết hóa đơn: " + e.getMessage());
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingInvoices(
            @RequestParam("page") Optional<Integer> pageNo,
            @RequestParam("limit") Optional<Integer> limit) {
        try {
            if (pageNo.isPresent() && pageNo.get() == 0) {
                return new ResponseEntity<>("Không tìm thấy trang", HttpStatus.NOT_FOUND);
            }

            Sort sort = Sort.by(Sort.Order.desc("id"));
            Pageable pageable = PageRequest.of(pageNo.orElse(1) - 1, limit.orElse(10), sort);

            Page<Invoice> page = invoiceDAO.findByStatus(InvoiceStatus.PENDING, pageable);

            List<InvoiceListDTO> dtoList = page.getContent().stream()
                    .map(invoice -> convertToListDTO(invoice, true))
                    .collect(Collectors.toList());

            ResponseDTO<InvoiceListDTO> responseDTO = new ResponseDTO<>();
            responseDTO.setData(dtoList);
            responseDTO.setTotalItems(page.getTotalElements());
            responseDTO.setTotalPages(page.getTotalPages());

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách hóa đơn chờ xử lý: " + e.getMessage());
        }
    }

    @GetMapping("/shipping")
    public ResponseEntity<?> getShippingInvoices(
            @RequestParam("page") Optional<Integer> pageNo,
            @RequestParam("limit") Optional<Integer> limit) {
        try {
            if (pageNo.isPresent() && pageNo.get() == 0) {
                return new ResponseEntity<>("Không tìm thấy trang", HttpStatus.NOT_FOUND);
            }

            Sort sort = Sort.by(Sort.Order.desc("id"));
            Pageable pageable = PageRequest.of(pageNo.orElse(1) - 1, limit.orElse(10), sort);

            Page<Invoice> page = invoiceDAO.findByStatus(InvoiceStatus.SHIPPING, pageable);

            List<InvoiceListDTO> dtoList = page.getContent().stream()
                    .map(invoice -> convertToListDTO(invoice, false))
                    .collect(Collectors.toList());

            ResponseDTO<InvoiceListDTO> responseDTO = new ResponseDTO<>();
            responseDTO.setData(dtoList);
            responseDTO.setTotalItems(page.getTotalElements());
            responseDTO.setTotalPages(page.getTotalPages());

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách hóa đơn đang giao: " + e.getMessage());
        }
    }

    @GetMapping("/delivered")
    public ResponseEntity<?> getDeliveredInvoices(
            @RequestParam("page") Optional<Integer> pageNo,
            @RequestParam("limit") Optional<Integer> limit) {
        try {
            if (pageNo.isPresent() && pageNo.get() == 0) {
                return new ResponseEntity<>("Không tìm thấy trang", HttpStatus.NOT_FOUND);
            }

            Sort sort = Sort.by(Sort.Order.desc("id"));
            Pageable pageable = PageRequest.of(pageNo.orElse(1) - 1, limit.orElse(10), sort);

            Page<Invoice> page = invoiceDAO.findByStatus(InvoiceStatus.DELIVERED, pageable);

            List<InvoiceListDTO> dtoList = page.getContent().stream()
                    .map(invoice -> convertToListDTO(invoice, false))
                    .collect(Collectors.toList());

            ResponseDTO<InvoiceListDTO> responseDTO = new ResponseDTO<>();
            responseDTO.setData(dtoList);
            responseDTO.setTotalItems(page.getTotalElements());
            responseDTO.setTotalPages(page.getTotalPages());

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách hóa đơn đã giao: " + e.getMessage());
        }
    }

    @GetMapping("/cancelled")
    public ResponseEntity<?> getCancelledInvoices(
            @RequestParam("page") Optional<Integer> pageNo,
            @RequestParam("limit") Optional<Integer> limit) {
        try {
            if (pageNo.isPresent() && pageNo.get() == 0) {
                return new ResponseEntity<>("Không tìm thấy trang", HttpStatus.NOT_FOUND);
            }

            Sort sort = Sort.by(Sort.Order.desc("id"));
            Pageable pageable = PageRequest.of(pageNo.orElse(1) - 1, limit.orElse(10), sort);

            Page<Invoice> page = invoiceDAO.findByStatus(InvoiceStatus.CANCELLED, pageable);

            List<InvoiceListDTO> dtoList = page.getContent().stream()
                    .map(invoice -> convertToListDTO(invoice, false))
                    .collect(Collectors.toList());

            ResponseDTO<InvoiceListDTO> responseDTO = new ResponseDTO<>();
            responseDTO.setData(dtoList);
            responseDTO.setTotalItems(page.getTotalElements());
            responseDTO.setTotalPages(page.getTotalPages());

            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi lấy danh sách hóa đơn đã hủy: " + e.getMessage());
        }
    }

    private InvoiceListDTO convertToListDTO(Invoice invoice, boolean includeCancelReason) {
        InvoiceListDTO dto = new InvoiceListDTO();
        dto.setInvoiceId("#" + invoice.getId());
        dto.setOrderDate(invoice.getOrderDate());
        dto.setAddress(invoice.getAddress());
        dto.setTotalAmount(invoice.getTotalAmount());
        dto.setStatus(invoice.getStatus().toString());
        dto.setCustomerName(invoice.getUser().getFullname());

        // 🚀 Debug để kiểm tra dữ liệu có đúng không
        System.out.println("Convert DTO - Cancel Reason: "
                + (invoice.getCancelReason() != null ? invoice.getCancelReason().getReason() : "NULL"));

        // ✅ Cách đúng để hiển thị `cancelReason`
        dto.setCancelReason(invoice.getCancelReason() != null ? invoice.getCancelReason().getReason() : null);

        return dto;
    }

    // ✅ API cập nhật trạng thái hóa đơn
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateInvoiceStatus(
            @PathVariable Integer id,
            @RequestBody UpdateStatusRequest request) {

        try {
            // Validate request
            if (request.getStatus() == null) {
                return ResponseEntity.badRequest().body("Status cannot be null");
            }

            // 🚀 Tìm hóa đơn cần cập nhật
            Invoice invoice = invoiceDAO.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn với ID: " + id));

            // Validate status transition
            if (!isValidStatusTransition(invoice.getStatus(), request.getStatus())) {
                return ResponseEntity.badRequest()
                        .body("Không thể chuyển từ trạng thái " + invoice.getStatus() +
                                " sang " + request.getStatus());
            }

            // 🚀 Cập nhật trạng thái
            invoice.setStatus(request.getStatus());

            // Set note if provided
            if (request.getNote() != null && !request.getNote().trim().isEmpty()) {
                invoice.setNote(request.getNote().trim());
            }

            // Nếu trạng thái là "CANCELLED", cần có lý do hủy
            if (request.getStatus() == InvoiceStatus.CANCELLED) {
                if (request.getCancelReasonId() == null) {
                    return ResponseEntity.badRequest().body("Vui lòng cung cấp lý do hủy.");
                }
                CancelReason cancelReason = cancelReasonDAO.findById(request.getCancelReasonId())
                        .orElseThrow(() -> new RuntimeException(
                                "Không tìm thấy lý do hủy với ID: " + request.getCancelReasonId()));
                invoice.setCancelReason(cancelReason);
            } else {
                // Nếu không phải "CANCELLED", xóa lý do hủy nếu có
                invoice.setCancelReason(null);
            }

            // 🚀 Lưu hóa đơn đã cập nhật
            invoiceDAO.save(invoice);
            return ResponseEntity.ok("Cập nhật trạng thái hóa đơn thành công.");

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật trạng thái hóa đơn: " + e.getMessage());
        }
    }

    private boolean isValidStatusTransition(InvoiceStatus currentStatus, InvoiceStatus newStatus) {
        if (currentStatus == newStatus) {
            return false;
        }

        switch (currentStatus) {
            case PENDING:
                return newStatus == InvoiceStatus.SHIPPING || newStatus == InvoiceStatus.CANCELLED;
            case SHIPPING:
                return newStatus == InvoiceStatus.DELIVERED || newStatus == InvoiceStatus.CANCELLED;
            case DELIVERED:
                return false; // Không thể thay đổi sau khi đã giao hàng
            case CANCELLED:
                return false; // Không thể thay đổi sau khi đã hủy
            default:
                return false;
        }
    }
}

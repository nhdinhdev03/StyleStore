package com.shopethethao.modules.invoices;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.shopethethao.modules.account.Account;
import com.shopethethao.modules.cancelReason.CancelReason;
import com.shopethethao.modules.detailed_invoices.DetailedInvoices;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "order_date", nullable = false, columnDefinition = "DATETIME DEFAULT GETDATE()")
    private LocalDateTime orderDate;

    @Column(name = "address", length = 200)
    private String address;

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "note", length = 200)
    private String note;

    @Column(name = "total_amount", nullable = false, precision = 18, scale = 2, columnDefinition = "DECIMAL(18,2) DEFAULT 0.00")
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "cancel_reason_id", foreignKey = @ForeignKey(name = "FK_Invoices_CancelReason"))
    private CancelReason cancelReason;
    
    @OneToMany(mappedBy = "invoice", fetch = FetchType.LAZY)
    private List<DetailedInvoices> detailedInvoices;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, 
                foreignKey = @ForeignKey(name = "FK_Invoices_User"),
                columnDefinition = "NVARCHAR(100)")
    private Account user;

    @PrePersist
    protected void onCreate() {
        if (orderDate == null) {
            orderDate = LocalDateTime.now();
        }
    }
}

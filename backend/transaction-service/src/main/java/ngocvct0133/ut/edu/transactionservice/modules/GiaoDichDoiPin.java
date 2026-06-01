package ngocvct0133.ut.edu.transactionservice.modules;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

@Entity
@Table(name = "giaodichdoipin")
public class GiaoDichDoiPin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long maGiaoDichDoiPin;
    @NotBlank(message = "maPinTra is required")
    private String maPinTra;
    @NotBlank(message = "maPinNhan is required")
    private String maPinNhan;
    @NotNull(message = "ngayGiaoDich is required")
    private LocalDateTime ngayGiaoDich;
    @NotBlank(message = "trangThaiGiaoDich is required")
    @Pattern(regexp = "^(Đang xử lý|Đã hoàn thành)$", message = "trangThaiGiaoDich is invalid")
    private String trangThaiGiaoDich;
    @NotNull(message = "thanhtien is required")
    private Double thanhtien;
    @NotBlank(message = "phuongThucThanhToan is required")
    @Pattern(regexp = "^(card|cash|package)$", message = "phuongThucThanhToan is invalid")
    private String phuongThucThanhToan;
    @NotNull(message = "maTram is required")
    private Long maTram;
    @NotNull(message = "maTaiXe is required")
    private Long maTaiXe;

    public GiaoDichDoiPin() {
    }

    public GiaoDichDoiPin(Long maGiaoDichDoiPin, String maPinTra, String maPinNhan, LocalDateTime ngayGiaoDich, String trangThaiGiaoDich, Double thanhtien, String phuongThucThanhToan) {
        this.maGiaoDichDoiPin = maGiaoDichDoiPin;
        this.maPinTra = maPinTra;
        this.maPinNhan = maPinNhan;
        this.ngayGiaoDich = ngayGiaoDich;
        this.trangThaiGiaoDich = trangThaiGiaoDich;
        this.thanhtien = thanhtien;
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public Long getMaTram() {
        return maTram;
    }

    public void setMaTram(Long maTram) {
        this.maTram = maTram;
    }

    public Long getMaTaiXe() {
        return maTaiXe;
    }

    public void setMaTaiXe(Long maTaiXe) {
        this.maTaiXe = maTaiXe;
    }

    public String getPhuongThucThanhToan() {
        return phuongThucThanhToan;
    }

    public void setPhuongThucThanhToan(String phuongThucThanhToan) {
        this.phuongThucThanhToan = phuongThucThanhToan;
    }

    public Double getThanhtien() {
        return thanhtien;
    }

    public void setThanhtien(Double thanhtien) {
        this.thanhtien = thanhtien;
    }

    public String getTrangThaiGiaoDich() {
        return trangThaiGiaoDich;
    }

    public void setTrangThaiGiaoDich(String trangThaiGiaoDich) {
        this.trangThaiGiaoDich = trangThaiGiaoDich;
    }

    public LocalDateTime getNgayGiaoDich() {
        return ngayGiaoDich;
    }

    public void setNgayGiaoDich(LocalDateTime ngayGiaoDich) {
        this.ngayGiaoDich = ngayGiaoDich;
    }

    public String getMaPinNhan() {
        return maPinNhan;
    }

    public void setMaPinNhan(String maPinNhan) {
        this.maPinNhan = maPinNhan;
    }

    public String getMaPinTra() {
        return maPinTra;
    }

    public void setMaPinTra(String maPinTra) {
        this.maPinTra = maPinTra;
    }

    public Long getMaGiaoDichDoiPin() {
        return maGiaoDichDoiPin;
    }

    public void setMaGiaoDichDoiPin(Long maGiaoDichDoiPin) {
        this.maGiaoDichDoiPin = maGiaoDichDoiPin;
    }
}

package datdq0317.edu.ut.vn.dinhquocdat.userservice.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import datdq0317.edu.ut.vn.dinhquocdat.userservice.dtos.TaiXeDTO;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.dtos.TaiXeResponse;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.models.NguoiDung;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.models.TaiXe;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.repositories.INguoiDungRepository;
import datdq0317.edu.ut.vn.dinhquocdat.userservice.repositories.ITaiXeRepository;
import jakarta.transaction.Transactional;

@Service
public class TaiXeService implements ITaiXeService{
    @Autowired
    private ITaiXeRepository taiXeRepository;

    @Autowired
    private INguoiDungRepository nguoiDungRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // Thêm PasswordEncoder

    @Override
    public TaiXe themTaiXe(TaiXeDTO dto) {
         // VALIDATE EMAIL
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
        throw new RuntimeException("Người dùng chưa cung cấp email");
        }
         // VALIDATE SỐ ĐIỆN THOẠI
        if (dto.getSoDienThoai() == null || dto.getSoDienThoai().trim().isEmpty()) {
        throw new RuntimeException("người dùng chưa cung cấp số điện thoại");
        }

        boolean phoneExists = nguoiDungRepository
        .findBySoDienThoai(dto.getSoDienThoai())
        .isPresent();

        boolean emailExists = nguoiDungRepository
        .findByEmail(dto.getEmail())
        .isPresent();

        if (phoneExists) {
        throw new RuntimeException("Số điện thoại đã tồn tại!");
        }

        if (emailExists) {
            throw new RuntimeException("Email đã tồn tại!");
        }
        // VALIDATE FORMAT SỐ ĐIỆN THOẠI
        if (!dto.getSoDienThoai().matches("^0\\d{9}$")) {
            throw new RuntimeException("Số điện thoại không hợp lệ");
        }
        // VALIDATE FORMAT EMAIL
        if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
        throw new RuntimeException("Định dạng email không hợp lệ");
        }
        // VALIDATE NGÀY SINH
        if (dto.getNgaySinh() == null) {
        throw new RuntimeException("Ngày sinh không hợp lệ");
        }

        // VALIDATE TUỔI >= 18
        if (dto.getNgaySinh().isAfter(LocalDate.now().minusYears(18))) {
        throw new RuntimeException("Tài xế phải từ 18 tuổi trở lên");
        }
        NguoiDung nd = new NguoiDung();
        nd.setHoTen(dto.getHoTen());
        nd.setEmail(dto.getEmail());
        nd.setSoDienThoai(dto.getSoDienThoai());
        nd.setGioiTinh(dto.getGioiTinh());

        // ENCODE MẬT KHẨU TRƯỚC KHI LƯU
        String encodedPassword = passwordEncoder.encode(dto.getMatKhau());
        nd.setMatKhau(encodedPassword);

        nd.setNgaySinh(dto.getNgaySinh());
        nd.setNgayTao(LocalDate.now());
        nd.setVaiTro("TAIXE");
        nguoiDungRepository.save(nd);

        TaiXe tx = new TaiXe();
        tx.setBangLaiXe(dto.getBangLaiXe());
        tx.setNguoiDung(nd);
        return taiXeRepository.save(tx);
    }

    @Override
    public TaiXe layTaiXeTheoMaNguoiDung(Long maNguoiDung) {
        return taiXeRepository.findByNguoiDung_MaNguoiDung(maNguoiDung);
    }

    @Override
    public List<TaiXe> danhSachTaiXe() {
        return taiXeRepository.findAll();
    }

    @Override
    public TaiXe layTaiXeTheoId(Long id) {
        return taiXeRepository.findById(id).orElse(null);
    }

   @Override
    @Transactional
    public boolean xoaTaiXe(Long id) {
    try {
        // Tìm tài xế trước
        TaiXe tx = taiXeRepository.findById(id).orElse(null);
        if (tx == null) {
            System.out.println("❌ Không tìm thấy tài xế với ID: " + id);
            return false;
        }
        
        // Lấy thông tin người dùng trước khi xóa
        NguoiDung nd = tx.getNguoiDung();
        
        System.out.println("✅ Tìm thấy tài xế: " + nd.getHoTen());
        System.out.println("✅ Mã người dùng: " + nd.getMaNguoiDung());
        
        // QUAN TRỌNG: Xóa quan hệ trước
        tx.setNguoiDung(null);  // Ngắt quan hệ
        taiXeRepository.save(tx); // Lưu thay đổi
        taiXeRepository.delete(tx);
        nguoiDungRepository.delete(nd);
        
       
        
        System.out.println("🎉 Xóa thành công!");
        return true;
    } catch (Exception e) {
        System.out.println("💥 Lỗi khi xóa: " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

    @Override
    public TaiXe suaTaiXe(Long id, TaiXeDTO dto) {
        return taiXeRepository.findById(id).map(tx -> {
            tx.setBangLaiXe(dto.getBangLaiXe());
            NguoiDung nd = tx.getNguoiDung();

            if (!nd.getEmail().equals(dto.getEmail())) {
                nguoiDungRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
                    if (!existing.getMaNguoiDung().equals(nd.getMaNguoiDung())) {
                        throw new RuntimeException("Email đã được sử dụng bởi người dùng khác!");
                    }
                });
                nd.setEmail(dto.getEmail());
            }

            if (!nd.getSoDienThoai().equals(dto.getSoDienThoai())) {
                nguoiDungRepository.findBySoDienThoai(dto.getSoDienThoai()).ifPresent(existing -> {
                    if (!existing.getMaNguoiDung().equals(nd.getMaNguoiDung())) {
                        throw new RuntimeException("Số điện thoại đã được sử dụng bởi người dùng khác!");
                    }
                });
                nd.setSoDienThoai(dto.getSoDienThoai());
            }

            // Cập nhật các field khác
            nd.setHoTen(dto.getHoTen());
            nd.setGioiTinh(dto.getGioiTinh());

            // ENCODE MẬT KHẨU KHI SỬA (nếu có thay đổi mật khẩu)
            if (dto.getMatKhau() != null && !dto.getMatKhau().isEmpty()) {
                String encodedPassword = passwordEncoder.encode(dto.getMatKhau());
                nd.setMatKhau(encodedPassword);
            }

            nd.setNgaySinh(dto.getNgaySinh());
            nguoiDungRepository.save(nd);

            return taiXeRepository.save(tx);
        }).orElseThrow(() -> new RuntimeException("Không tìm thấy tài xế!"));
    }
    @Override
public TaiXeResponse layThongTinTaiXe(Long id) {
    TaiXe tx = taiXeRepository.findById(id)
            .orElse(null);

    if (tx == null) {
        return null;
    }

    NguoiDung nd = tx.getNguoiDung();

    TaiXeResponse res = new TaiXeResponse();
    res.setId(tx.getMaTaiXe());
    res.setHoTen(nd.getHoTen());
    res.setEmail(nd.getEmail());
    res.setSoDienThoai(nd.getSoDienThoai());
    res.setGioiTinh(nd.getGioiTinh());
    res.setNgaySinh(nd.getNgaySinh());
    res.setBangLaiXe(tx.getBangLaiXe());

    return res;
}

}
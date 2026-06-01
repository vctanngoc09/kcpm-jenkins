package luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.services;

import jakarta.transaction.Transactional;
import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.dtos.PhuongTienDTO;
import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.modules.PhuongTien;
import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.repositories.IPhuongTienRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class PhuongTienService implements IPhuongTienService {

    @Autowired
    private IPhuongTienRepository phuongTienRepository;

    private static final String LICENSE_PLATE_REGEX = "^\\d{2}[A-Z]-\\d{4,5}$";

    @Transactional
    @Override
    public PhuongTien themPhuongTien(PhuongTienDTO dto) {
        validateCreate(dto);

        if (phuongTienRepository.existsByVin(dto.getVin().trim())) {
            throw new IllegalArgumentException("VIN đã tồn tại");
        }

        if (phuongTienRepository.existsByBienSo(dto.getBienSo().trim())) {
            throw new IllegalArgumentException("Biển số đã tồn tại");
        }

        PhuongTien v = new PhuongTien();
        v.setVin(dto.getVin().trim());
        v.setBienSo(dto.getBienSo().trim().toUpperCase());
        v.setLoaiXe(dto.getLoaiXe().trim());
        v.setMaTaiXe(dto.getMaTaiXe());

        // Theo test Postman: tạo xe chưa được gắn pin.
        // Muốn gắn pin thì dùng API /link-pin/{pinId}
        v.setMaPin(null);

        return phuongTienRepository.save(v);
    }

    @Override
    public List<PhuongTien> danhSachPhuongTien() {
        return phuongTienRepository.findAll();
    }

    @Override
    public PhuongTien layPhuongTienTheoId(Long id) {
        return phuongTienRepository.findById(id).orElse(null);
    }

    @Override
    public boolean xoaPhuongTien(Long id) {
        if (!phuongTienRepository.existsById(id)) {
            return false;
        }

        phuongTienRepository.deleteById(id);
        return true;
    }

    @Transactional
    @Override
    public PhuongTien suaPhuongTien(Long id, PhuongTienDTO dto) {
        PhuongTien v = phuongTienRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phương tiện"));

        validateUpdate(dto);

        if (dto.getVin() != null && !dto.getVin().trim().equals(v.getVin())) {
            String newVin = dto.getVin().trim();

            if (phuongTienRepository.existsByVinAndMaPhuongTienNot(newVin, id)) {
                throw new IllegalArgumentException("VIN đã tồn tại");
            }

            v.setVin(newVin);
        }

        if (dto.getBienSo() != null && !dto.getBienSo().trim().equalsIgnoreCase(v.getBienSo())) {
            String newBienSo = dto.getBienSo().trim().toUpperCase();

            if (phuongTienRepository.existsByBienSoAndMaPhuongTienNot(newBienSo, id)) {
                throw new IllegalArgumentException("Biển số đã tồn tại");
            }

            v.setBienSo(newBienSo);
        }

        if (dto.getLoaiXe() != null) {
            if (isBlank(dto.getLoaiXe())) {
                throw new IllegalArgumentException("Loại xe không được để trống");
            }
            v.setLoaiXe(dto.getLoaiXe().trim());
        }

        if (dto.getMaTaiXe() != null) {
            v.setMaTaiXe(dto.getMaTaiXe());
        }

        // Không update maPin ở API update vehicle.
        // maPin chỉ xử lý ở link-pin / unlink-pin.

        return phuongTienRepository.save(v);
    }

    @Override
    public List<PhuongTien> danhSachTheoTaiXe(Long maTaiXe) {
        return phuongTienRepository.findAllByMaTaiXe(maTaiXe);
    }

    @Transactional
    @Override
    public PhuongTien lienKetPin(Long maPhuongTien, Long maPin) {
        PhuongTien v = phuongTienRepository.findById(maPhuongTien)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phương tiện"));

        if (maPin == null) {
            throw new IllegalArgumentException("Mã pin không được để trống");
        }

        v.setMaPin(maPin);
        return phuongTienRepository.save(v);
    }

    @Transactional
    @Override
    public PhuongTien huyLienKetPin(Long maPhuongTien) {
        PhuongTien v = phuongTienRepository.findById(maPhuongTien)
                .orElseThrow(() -> new NoSuchElementException("Không tìm thấy phương tiện"));

        v.setMaPin(null);
        return phuongTienRepository.save(v);
    }

    private void validateCreate(PhuongTienDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu JSON không hợp lệ");
        }

        if (isBlank(dto.getVin())) {
            throw new IllegalArgumentException("VIN không được để trống");
        }

        if (isBlank(dto.getBienSo())) {
            throw new IllegalArgumentException("Biển số không được để trống");
        }

        if (isBlank(dto.getLoaiXe())) {
            throw new IllegalArgumentException("Loại xe không được để trống");
        }

        if (dto.getMaTaiXe() == null) {
            throw new IllegalArgumentException("Mã tài xế không được để trống");
        }

        validateLicensePlate(dto.getBienSo());
    }

    private void validateUpdate(PhuongTienDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Dữ liệu JSON không hợp lệ");
        }

        if (dto.getVin() != null && isBlank(dto.getVin())) {
            throw new IllegalArgumentException("VIN không được để trống");
        }

        if (dto.getBienSo() != null) {
            if (isBlank(dto.getBienSo())) {
                throw new IllegalArgumentException("Biển số không được để trống");
            }
            validateLicensePlate(dto.getBienSo());
        }
    }

    private void validateLicensePlate(String bienSo) {
        String value = bienSo.trim().toUpperCase();

        if (!value.matches(LICENSE_PLATE_REGEX)) {
            throw new IllegalArgumentException("Dữ liệu JSON sai định dạng hoặc biển số không hợp lệ");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
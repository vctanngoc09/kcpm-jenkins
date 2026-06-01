package ut.edu.stationservice.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import ut.edu.stationservice.models.Tram;
import ut.edu.stationservice.repositories.ITramRepository;

@Service
public class TramService implements ITramService {

    @Autowired
    private ITramRepository tramRepository;

    @Transactional
    @Override
    public Tram addPin(Tram tram) {
        if (tram.getTenTram() == null || tram.getTenTram().trim().isEmpty()) {
            throw new RuntimeException("Tên trạm rỗng");
        }
        if (tramRepository.existsByTenTram(tram.getTenTram().trim())) {
            throw new RuntimeException("Tên trạm trùng");
        }

        if (tram.getTenTram().length() > 150) {
            throw new RuntimeException("Tên trạm lố 150 kí tự");
        }

        if (tram.getDiaChi() == null || tram.getDiaChi().trim().isEmpty()) {
            throw new RuntimeException("Địa chỉ rỗng");
        }

        if (tram.getDiaChi().length() > 250) {
            throw new RuntimeException("Địa chỉ lố 250 kí tự");
        }

        if (tram.getKinhDo() == null) {
            throw new RuntimeException("kinh độ bị rỗng");
        }
        if (tram.getKinhDo() < -180 || tram.getKinhDo() > 180) {
            throw new RuntimeException("kinh độ vượt biên (>180)");
        }

        if (tram.getViDo() == null) {
            throw new RuntimeException("Vĩ độ bị rỗng");
        }
        if (tram.getViDo() < -90 || tram.getViDo() > 90) {
            throw new RuntimeException("vĩ độ vượt biên (>90)");
        }

        if (tram.getSoLuongPinToiDa() == null) {
            throw new RuntimeException("pin rỗng"); // Khớp với test [pin rỗng]
        }
        if (tram.getSoLuongPinToiDa() < 0) {
            throw new RuntimeException("pin âm"); // Khớp với test [pin âm]
        }

        // 2. Kiểm tra số điện thoại
        if (tram.getSoDT() == null || tram.getSoDT().trim().isEmpty()) {
            throw new RuntimeException("số điện thoại rỗng"); // Khớp với test [số điện thoại rỗng]
        }

        tram.setTenTram(tram.getTenTram().trim());
        return tramRepository.save(tram);
    }

    @Transactional
    @Override
    public Tram updatePin(Tram tram) {
        return tramRepository.findById(tram.getMaTram())
                .map(existing -> {
                    if (!existing.getTenTram().equals(tram.getTenTram())
                            && tramRepository.existsByTenTram(tram.getTenTram())) {
                        throw new RuntimeException("Tên trạm đã được sử dụng!");
                    }

                    existing.setTenTram(tram.getTenTram());
                    existing.setDiaChi(tram.getDiaChi());
                    existing.setKinhDo(tram.getKinhDo());
                    existing.setViDo(tram.getViDo());
                    existing.setSoLuongPinToiDa(tram.getSoLuongPinToiDa());
                    existing.setSoDT(tram.getSoDT());
                    existing.setTrangThai(tram.getTrangThai());
                    return tramRepository.save(existing);
                })
                .orElseThrow(() -> new RuntimeException("Không tìm thấy trạm!"));
    }

    @Override
    public boolean deleteById(Long id) {
        if (!tramRepository.existsById(id)) return false;
        tramRepository.deleteById(id);
        return true;
    }

    @Override
    public List<Tram> findByTramId(Long tramId) {
        // Nếu cần tìm theo mã trạm — có thể thêm query custom sau
        return tramRepository.findAll();
    }

    @Override
    public Tram findById(Long id) {
        return tramRepository.findById(id).orElse(null);
    }

    @Transactional
    @Override
    public Tram save(Tram tram) {
        return tramRepository.save(tram);
    }

    @Override
    public List<Tram> addNhieuTram(List<Tram> dsTram) {
        return tramRepository.saveAll(dsTram);
    }
}

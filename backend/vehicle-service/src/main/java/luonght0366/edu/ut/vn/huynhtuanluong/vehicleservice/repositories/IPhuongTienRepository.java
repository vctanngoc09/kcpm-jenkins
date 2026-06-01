package luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.repositories;

import luonght0366.edu.ut.vn.huynhtuanluong.vehicleservice.modules.PhuongTien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IPhuongTienRepository extends JpaRepository<PhuongTien, Long> {

    List<PhuongTien> findAllByMaTaiXe(Long maTaiXe);

    boolean existsByVin(String vin);

    boolean existsByBienSo(String bienSo);

    boolean existsByVinAndMaPhuongTienNot(String vin, Long maPhuongTien);

    boolean existsByBienSoAndMaPhuongTienNot(String bienSo, Long maPhuongTien);
}
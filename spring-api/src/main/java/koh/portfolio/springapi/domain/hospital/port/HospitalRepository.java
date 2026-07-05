package koh.portfolio.springapi.domain.hospital.port;

import koh.portfolio.springapi.domain.hospital.model.Hospital;

import java.util.List;
import java.util.Optional;

public interface HospitalRepository {
    Hospital save(Hospital hospital);
    Optional<Hospital> findById(Long id);
    List<Hospital> search(String keyword, String area);
}

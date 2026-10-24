package koh.portfolio.springapi.application.analysis.service;

import koh.portfolio.springapi.common.exception.Preconditions;
import koh.portfolio.springapi.domain.analysis.exception.AnalysisErrorCode;
import koh.portfolio.springapi.domain.pet.model.Pet;
import koh.portfolio.springapi.domain.pet.port.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

// 分析APIは飼い主本人のペットだけを対象にする（健康記録APIと同じ判定。削除済みのペットは存在しない扱い）
@Component
@RequiredArgsConstructor
public class AnalysisPetChecker {
    private final PetRepository petRepository;

    public void checkOwner(Long userId, Long petId) {
        Pet pet = petRepository.findById(petId).orElse(null);

        Preconditions.validate(
                pet != null && pet.getUserId().equals(userId),
                AnalysisErrorCode.NOT_PET_OWNER
        );
    }
}

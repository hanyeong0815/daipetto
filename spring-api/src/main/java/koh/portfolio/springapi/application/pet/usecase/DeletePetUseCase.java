package koh.portfolio.springapi.application.pet.usecase;

public interface DeletePetUseCase {
    void execute(Long userId, Long petId);
}

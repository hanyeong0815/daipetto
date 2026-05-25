package koh.portfolio.springapi.infrastructure.mapper;

public interface DomainEntityMapper<DOMAIN, ENTITY> {
    DOMAIN toDomain(ENTITY entity);
    ENTITY toEntity(DOMAIN domain);
}

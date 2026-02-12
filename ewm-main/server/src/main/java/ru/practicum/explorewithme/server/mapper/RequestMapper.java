package ru.practicum.explorewithme.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.explorewithme.request.dto.ParticipationRequestDto;
import ru.practicum.explorewithme.server.entity.Request;

@Mapper(componentModel = "spring")
public interface RequestMapper {

    @Mapping(source = "event.id", target = "event")
    @Mapping(source = "requester.id", target = "requester")
    @Mapping(target = "status", expression = "java(request.getStatus().name())")
    ParticipationRequestDto toDto(Request request);
}

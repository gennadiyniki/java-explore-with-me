package ru.practicum.explorewithme.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.explorewithme.event.dto.EventFullDto;
import ru.practicum.explorewithme.event.dto.EventShortDto;
import ru.practicum.explorewithme.event.dto.Location;
import ru.practicum.explorewithme.server.entity.Event;
import ru.practicum.explorewithme.server.entity.EventLocation;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class, UserMapper.class})
public interface EventMapper {

    @Mapping(source = "event.id", target = "id")
    @Mapping(source = "event.annotation", target = "annotation")
    @Mapping(source = "event.category", target = "category")
    @Mapping(source = "event.createdOn", target = "createdOn")
    @Mapping(source = "event.description", target = "description")
    @Mapping(source = "event.eventDate", target = "eventDate")
    @Mapping(source = "event.initiator", target = "initiator")
    @Mapping(source = "event.location", target = "location")
    @Mapping(source = "event.paid", target = "paid")
    @Mapping(source = "event.participantLimit", target = "participantLimit")
    @Mapping(source = "event.publishedOn", target = "publishedOn")
    @Mapping(source = "event.requestModeration", target = "requestModeration")
    @Mapping(target = "state", expression = "java(event.getState().name())")
    @Mapping(source = "event.title", target = "title")
    @Mapping(source = "confirmedRequests", target = "confirmedRequests")
    @Mapping(source = "views", target = "views")
    @Mapping(source = "commentCount", target = "commentCount")  // Добавлено
    EventFullDto toFullDto(Event event, Long confirmedRequests, Long views, Long commentCount, boolean isNew);

    @Mapping(source = "event.id", target = "id")
    @Mapping(source = "event.annotation", target = "annotation")
    @Mapping(source = "event.category", target = "category")
    @Mapping(source = "event.eventDate", target = "eventDate")
    @Mapping(source = "event.initiator", target = "initiator")
    @Mapping(source = "event.paid", target = "paid")
    @Mapping(source = "event.title", target = "title")
    @Mapping(source = "confirmedRequests", target = "confirmedRequests")
    @Mapping(source = "views", target = "views")
    @Mapping(source = "commentCount", target = "commentCount")  // Добавлено
    EventShortDto toShortDto(Event event, Long confirmedRequests, Long views, Long commentCount);

    Location toLocationDto(EventLocation location);
}
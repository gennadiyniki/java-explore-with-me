package ru.practicum.explorewithme.server.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.explorewithme.comment.dto.CommentDto;
import ru.practicum.explorewithme.server.entity.Comment;
import ru.practicum.explorewithme.server.entity.Event;
import ru.practicum.explorewithme.server.entity.User;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "text", source = "text")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "author", source = "author")
    @Mapping(target = "createdAt", expression = "java(java.time.LocalDateTime.now())")
    Comment toEntity(String text, Event event, User author);

    @Mapping(source = "comment.id", target = "id")
    @Mapping(source = "comment.text", target = "text")
    @Mapping(source = "comment.createdAt", target = "createdAt")
    @Mapping(source = "comment.author.id", target = "authorId")
    @Mapping(source = "comment.author.name", target = "authorName")
    @Mapping(source = "comment.event.id", target = "eventId")
    CommentDto toDto(Comment comment);
}
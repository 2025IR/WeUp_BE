package com.example.weup.service;

import com.example.weup.GeneralException;
import com.example.weup.constant.ErrorInfo;
import com.example.weup.dto.request.CreateTodoRequestDTO;
import com.example.weup.dto.request.EditTodoRequestDTO;
import com.example.weup.dto.request.EditTodoStatusRequestDTO;
import com.example.weup.dto.response.TodoAssigneeResponseDTO;
import com.example.weup.dto.response.TodoListResponseDTO;
import com.example.weup.entity.*;
import com.example.weup.repository.MemberRepository;
import com.example.weup.repository.ProjectRepository;
import com.example.weup.repository.TodoMemberRepository;
import com.example.weup.repository.TodoRepository;
import com.example.weup.validate.MemberValidator;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TodoService {

    private final S3Service s3Service;
    private final MemberValidator memberValidator;
    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final TodoRepository todoRepository;
    private final TodoMemberRepository todoMemberRepository;

    @Transactional
    public void createTodo(Long userId, CreateTodoRequestDTO createTodoRequestDTO) {

        memberValidator.validateActiveMemberInProject(userId, createTodoRequestDTO.getProjectId());

        Project project = projectRepository.findById(createTodoRequestDTO.getProjectId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.PROJECT_NOT_FOUND));

        Todo todo = Todo.builder()
                .project(project)
                .build();

        todoRepository.save(todo);
    }

    public List<TodoListResponseDTO> getTodoList(Long userId, Long projectId) {

        Member requestMember = memberValidator.validateActiveMemberInProject(userId, projectId);

        List<Todo> todos = todoRepository.findByProject_ProjectId(projectId);

        return todos.stream()
                .map(todo -> {
                    List<TodoAssigneeResponseDTO> assignees = todo.getTodoMembers().stream()
                            .map(tm -> {
                                Member member = tm.getMember();
                                User user = member.getUser();

                                return TodoAssigneeResponseDTO.builder()
                                        .memberId(member.getMemberId())
                                        .name(user.getName())
                                        .profileImage(s3Service.getPresignedUrl(user.getProfileImage()))
                                        .build();
                            })
                            .collect(Collectors.toList());

                    boolean isMyTodo = assignees.stream()
                            .anyMatch(a -> a.getMemberId().equals(requestMember.getMemberId()));

                    Optional<TodoAssigneeResponseDTO> requesterDtoOpt = assignees.stream()
                            .filter(a -> a.getMemberId().equals(requestMember.getMemberId()))
                            .findFirst();

                    requesterDtoOpt.ifPresent(requesterDto -> {
                        assignees.remove(requesterDto);
                        assignees.add(0, requesterDto);
                    });

                    return TodoListResponseDTO.builder()
                            .todoId(todo.getTodoId())
                            .todoName(todo.getTodoName())
                            .startDate(todo.getStartDate())
                            .endDate(todo.getEndDate())
                            .status(todo.getTodoStatus())
                            .isMyTodo(isMyTodo)
                            .assignee(assignees)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void editTodo(Long userId, EditTodoRequestDTO editTodoRequestDTO) {

        Todo todo = todoRepository.findById(editTodoRequestDTO.getTodoId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.TODO_NOT_FOUND));

        memberValidator.validateActiveMemberInProject(userId, todo.getProject().getProjectId());

        if (editTodoRequestDTO.getTodoName() != null) {
            todo.setTodoName(editTodoRequestDTO.getTodoName());
        }

        if (editTodoRequestDTO.getStartDate() != null && editTodoRequestDTO.getEndDate() != null) {
            todo.setStartDate(editTodoRequestDTO.getStartDate());
            todo.setEndDate(editTodoRequestDTO.getEndDate());
        }

        todoRepository.save(todo);

        if (editTodoRequestDTO.getMemberIds() != null) {
            todoMemberRepository.deleteByTodo_TodoId(editTodoRequestDTO.getTodoId());
            todoMemberRepository.findAllByTodo_TodoId(editTodoRequestDTO.getTodoId());

            editTodoRequestDTO.getMemberIds().stream()
                    .map(memberId -> memberRepository.findById(memberId)
                            .orElseThrow(() -> new GeneralException(ErrorInfo.MEMBER_NOT_FOUND)))
                    .map(member -> TodoMember.builder()
                            .todo(todo)
                            .member(member)
                            .build())
                    .forEach(todoMemberRepository::save);
        }
    }



    @Transactional
    public void editTodoStatus(Long userId, EditTodoStatusRequestDTO editTodoStatusRequestDTO) {

        log.debug("edit todo status service in");
        log.debug("todo id : {}", editTodoStatusRequestDTO.getTodoId());
        log.debug("status : {}", editTodoStatusRequestDTO.getStatus());

        Todo todo = todoRepository.findById(editTodoStatusRequestDTO.getTodoId())
                .orElseThrow(() -> new GeneralException(ErrorInfo.TODO_NOT_FOUND));

        memberValidator.validateActiveMemberInProject(userId, todo.getProject().getProjectId());

        todo.setTodoStatus(editTodoStatusRequestDTO.getStatus());
        todoRepository.save(todo);
    }

    @Transactional
    public void deleteTodo(Long userId, Long todoId) {

        Todo todo = todoRepository.findById(todoId)
                .orElseThrow(() -> new GeneralException(ErrorInfo.TODO_NOT_FOUND));

        memberValidator.validateActiveMemberInProject(userId, todo.getProject().getProjectId());

        todoMemberRepository.deleteAllByTodo_TodoId(todoId);

        todoRepository.deleteById(todoId);
    }
}

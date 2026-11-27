package dev.wakey.spaces.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

class SpaceMembersControllerPlacementTest {

    @Test
    void theMembersOfASpaceAreOneController() {
        // when
        RequestMapping mapping = Objects.requireNonNull(
                AnnotatedElementUtils.findMergedAnnotation(SpaceMembersController.class, RequestMapping.class));
        List<String> methodNames = Arrays.stream(SpaceMembersController.class.getDeclaredMethods())
                .map(Method::getName)
                .toList();

        // then
        assertThat(mapping.path()).containsExactly(SpacesHttp.MEMBERS_PATH);
        assertThat(methodNames)
                .containsExactlyInAnyOrder(
                        "listSpaceMembers", "addSpaceMember", "changeSpaceMemberRole", "removeSpaceMember");
        assertMappedMethod("listSpaceMembers", RequestMethod.GET);
        assertMappedMethod("addSpaceMember", RequestMethod.POST);
        assertMappedMethod("changeSpaceMemberRole", RequestMethod.PUT);
        assertMappedMethod("removeSpaceMember", RequestMethod.DELETE);
        assertThatThrownBy(() -> Class.forName("dev.wakey.spaces.web.SpaceMemberRolesController"))
                .isInstanceOf(ClassNotFoundException.class);
    }

    private static void assertMappedMethod(String name, RequestMethod requestMethod) {
        Method method = Arrays.stream(SpaceMembersController.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name))
                .findFirst()
                .orElseThrow();
        RequestMapping mapping =
                Objects.requireNonNull(AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class));
        assertThat(mapping.method()).containsExactly(requestMethod);
    }
}

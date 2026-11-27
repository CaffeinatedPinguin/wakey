package dev.wakey.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.wakey.settings.internal.PlanCatalog;
import dev.wakey.spaces.SpaceMember;
import dev.wakey.spaces.SpaceMembers;
import dev.wakey.spaces.SpaceRole;
import dev.wakey.spaces.SpaceService;
import dev.wakey.spaces.SpaceView;
import dev.wakey.testsupport.PostgresIntegrationTest;
import dev.wakey.web.ApiException;
import dev.wakey.web.CommonError;
import dev.wakey.web.CurrentUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlanAssignmentTest extends PostgresIntegrationTest {

    private static final String ISSUER = "https://issuer.example.com";

    private static final CurrentUser ADMIN = new CurrentUser(ISSUER, "plan-admin", true);

    @Test
    void assigningAPlanChecksTheAdminRuleOnce() {
        // given a member who is not an application Admin
        SpaceService spaces = bean(SpaceService.class);
        PlanAssignment assignments = bean(PlanAssignment.class);
        String slug = uniqueSlug("plan-once");
        spaces.put(ADMIN, slug, "Plan once", null);
        bean(SpaceMembers.class)
                .addMember(ADMIN, slug, new SpaceMember(ISSUER, "plan-editor", SpaceRole.EDITOR), false);
        CurrentUser editor = new CurrentUser(ISSUER, "plan-editor", false);

        // when
        Throwable rejected = catchThrowable(() -> assignments.assign(editor, slug, "default", false));

        // then a non-admin is rejected by the writer, and the settings use case only delegates to it
        assertThat(rejected).isInstanceOf(ApiException.class);
        assertThat(((ApiException) rejected).getErrorCode()).isEqualTo(CommonError.FORBIDDEN);
        assertDelegatesTheAdminRuleToTheWriter();
    }

    private static void assertDelegatesTheAdminRuleToTheWriter() {
        SpaceService writer = mock(SpaceService.class);
        PlanCatalog plans = mock(PlanCatalog.class);
        PlanAssignment assignments = new PlanAssignment(writer, plans, mock(SettingAudit.class));
        CurrentUser editor = mock(CurrentUser.class);
        SpaceView view = mock(SpaceView.class);
        when(writer.view(editor, "space")).thenReturn(view);
        when(plans.nameOrDefault(null)).thenReturn("small");
        when(plans.requireExisting("small")).thenReturn("small");

        assignments.assign(editor, "space", "small", false);

        verify(editor, never()).requireAdmin();
        verify(writer).assignPlan(editor, "space", "small");
    }

    private static String uniqueSlug(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}

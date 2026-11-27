package dev.wakey.spaces.web;

import dev.wakey.spaces.SpaceService;
import dev.wakey.web.CurrentUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers the space actor argument resolver and hides its types from the public contract. */
@Configuration(proxyBeanMethods = false)
@RequiredArgsConstructor
class SpaceActorWebConfiguration implements WebMvcConfigurer {

    static {
        SpringDocUtils.getConfig()
                .addRequestWrapperToIgnore(
                        CurrentUser.class,
                        SpaceReader.class,
                        SpaceWriter.class,
                        SpaceUpsert.class,
                        SessionUser.class,
                        SpaceCaller.class);
    }

    private final SpaceService spaces;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        SpaceActors actors = new SpaceActors(spaces);
        resolvers.add(new SpaceActorArgumentResolver(actors));
    }
}

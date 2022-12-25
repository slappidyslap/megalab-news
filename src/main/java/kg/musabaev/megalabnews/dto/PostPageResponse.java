package kg.musabaev.megalabnews.dto;

import kg.musabaev.megalabnews.repository.projection.PostWithoutContent;
import org.springframework.data.domain.Page;

public record PostPageResponse(Page<PostWithoutContent> page) {
}

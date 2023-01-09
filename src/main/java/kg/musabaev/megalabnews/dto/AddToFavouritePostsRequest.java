package kg.musabaev.megalabnews.dto;

import jakarta.validation.constraints.Positive;

public record AddToFavouritePostsRequest(@Positive Long postId) {
}

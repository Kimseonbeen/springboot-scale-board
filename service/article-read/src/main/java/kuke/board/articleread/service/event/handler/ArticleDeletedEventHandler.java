package kuke.board.articleread.service.event.handler;

import kuke.board.articleread.repository.ArticleIdListRepository;
import kuke.board.articleread.repository.ArticleQueryModelRepository;
import kuke.board.articleread.repository.BoardArticleCountRepository;
import kuke.board.common.event.Event;
import kuke.board.common.event.EventType;
import kuke.board.common.event.payload.ArticleDeletedEventPayload;
import kuke.board.common.event.payload.ArticleUpdatedEventPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArticleDeletedEventHandler implements EventHandler<ArticleDeletedEventPayload> {
    private final ArticleIdListRepository articleIdListRepository;
    private final ArticleQueryModelRepository articleQueryModelRepository;
    private final BoardArticleCountRepository boardArticleCountRepository;

    @Override
    public void handle(Event<ArticleDeletedEventPayload> event) {
        ArticleDeletedEventPayload payload = event.getPayload();
        // 찰나의 순간에 목록 삭제전 데이터가 보일 수 있기 떄문에 진입점을 먼저 없엔다.
        articleIdListRepository.delete(payload.getBoardId(), payload.getArticleId());
        articleQueryModelRepository.delete(payload.getArticleId());
        boardArticleCountRepository.createOrUpdate(payload.getBoardId(), payload.getBoardArticleCount());
    }

    @Override
    public boolean supports(Event<ArticleDeletedEventPayload> event) {
        return EventType.ARTICLE_DELETED == event.getType();
    }
}

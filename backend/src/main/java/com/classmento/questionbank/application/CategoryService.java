package com.classmento.questionbank.application;

import com.classmento.common.domain.DomainException;
import com.classmento.common.web.ApiException;
import com.classmento.questionbank.domain.Category;
import com.classmento.questionbank.domain.CategoryRepository;
import com.classmento.questionbank.domain.QuestionSearchRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카테고리 관리 유스케이스(관리자). 같은 부모 아래 이름 중복은 여기서 확인한다(DB 유니크 인덱스가 최종 안전장치). */
@Service
public class CategoryService {

  private static final Comparator<Category> ORDER =
      Comparator.comparingInt(Category::getSortOrder).thenComparing(Category::getId);

  private final CategoryRepository categories;
  private final QuestionSearchRepository questionSearch;

  public CategoryService(CategoryRepository categories, QuestionSearchRepository questionSearch) {
    this.categories = categories;
    this.questionSearch = questionSearch;
  }

  /** 대분류(정렬순) → 소분류(정렬순) + 소분류별 질문 수. 숨긴 것도 포함한다(관리 화면). */
  @Transactional(readOnly = true)
  public List<CategoryNode> tree() {
    List<Category> all = categories.findAll();
    Map<Long, Long> counts = questionSearch.countByCategory();
    return all.stream()
        .filter(Category::isTop)
        .sorted(ORDER)
        .map(
            top ->
                new CategoryNode(
                    top,
                    all.stream()
                        .filter(c -> Objects.equals(c.getParentId(), top.getId()))
                        .sorted(ORDER)
                        .toList(),
                    counts))
        .toList();
  }

  @Transactional
  public Category create(Long parentId, String name) {
    Category parent =
        parentId == null ? null : categories.findById(parentId).orElseThrow(ApiException::notFound);
    List<Category> siblings = siblingsOf(parentId);
    requireUniqueName(siblings, name, null);
    int nextOrder = siblings.stream().mapToInt(Category::getSortOrder).max().orElse(-1) + 1;
    Category created =
        parent == null ? Category.top(name, nextOrder) : Category.child(parent, name, nextOrder);
    return categories.save(created);
  }

  @Transactional
  public Category update(Long id, String name, Integer sortOrder, Boolean active) {
    Category category = categories.findById(id).orElseThrow(ApiException::notFound);
    if (name != null) {
      requireUniqueName(siblingsOf(category.getParentId()), name, id);
      category.rename(name);
    }
    if (sortOrder != null) {
      category.reorder(sortOrder);
    }
    if (Boolean.TRUE.equals(active)) {
      category.show();
    } else if (Boolean.FALSE.equals(active)) {
      category.hide();
    }
    return category;
  }

  private List<Category> siblingsOf(Long parentId) {
    return categories.findAll().stream()
        .filter(c -> Objects.equals(c.getParentId(), parentId))
        .toList();
  }

  private static void requireUniqueName(List<Category> siblings, String name, Long selfId) {
    String key = Category.nameKey(name);
    boolean taken =
        siblings.stream()
            .anyMatch(c -> !c.getId().equals(selfId) && Category.nameKey(c.getName()).equals(key));
    if (taken) {
      throw new DomainException("CATEGORY_DUPLICATE", "같은 이름의 카테고리가 이미 있어요.");
    }
  }

  /** 대분류와 그 소분류. questionCounts: 소분류 ID → 질문 수(없으면 0). */
  public record CategoryNode(
      Category top, List<Category> children, Map<Long, Long> questionCounts) {

    public long questionCount(Category sub) {
      return questionCounts.getOrDefault(sub.getId(), 0L);
    }
  }
}

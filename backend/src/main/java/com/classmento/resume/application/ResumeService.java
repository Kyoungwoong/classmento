package com.classmento.resume.application;

import com.classmento.common.web.ApiException;
import com.classmento.resume.application.ResumeDtos.ExperienceView;
import com.classmento.resume.application.ResumeDtos.ResumeDetail;
import com.classmento.resume.application.ResumeDtos.ResumeSummary;
import com.classmento.resume.domain.ParsedResume;
import com.classmento.resume.domain.Resume;
import com.classmento.resume.domain.ResumeExperienceRepository;
import com.classmento.resume.domain.ResumeFileType;
import com.classmento.resume.domain.ResumeRepository;
import com.classmento.storage.FileStorage;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

@Service
public class ResumeService {

  private final ResumeRepository resumes;
  private final FileStorage storage;
  private final ResumeProcessor processor;
  private final TaskExecutor resumeExecutor;
  private final ObjectMapper json;
  private final ExperienceSync experienceSync;
  private final ResumeExperienceRepository experiences;
  private final TransactionTemplate tx;

  public ResumeService(
      ResumeRepository resumes,
      FileStorage storage,
      ResumeProcessor processor,
      TaskExecutor resumeExecutor,
      ObjectMapper json,
      ExperienceSync experienceSync,
      ResumeExperienceRepository experiences,
      TransactionTemplate tx) {
    this.resumes = resumes;
    this.storage = storage;
    this.processor = processor;
    this.resumeExecutor = resumeExecutor;
    this.json = json;
    this.experienceSync = experienceSync;
    this.experiences = experiences;
    this.tx = tx;
  }

  /** 파일을 저장하고 즉시 반환한다. 추출·임베딩·구조화는 resumeExecutor에서 이어서 처리한다. */
  public ResumeDetail upload(Long userId, MultipartFile file) {
    if (file == null || file.isEmpty()) {
      throw ApiException.badRequest("파일을 선택해 주세요.");
    }
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException e) {
      throw ApiException.badRequest("파일을 읽지 못했어요. 다시 올려주세요.");
    }
    ResumeFileType type =
        ResumeFileType.detect(file.getOriginalFilename(), Arrays.copyOf(bytes, 8))
            .orElseThrow(() -> ApiException.badRequest("PDF나 DOCX 파일만 올릴 수 있어요."));

    String key = storage.store("resumes/" + userId, file.getOriginalFilename(), bytes);
    Resume saved =
        resumes.save(
            new Resume(userId, file.getOriginalFilename(), type.contentType(), bytes.length, key));

    Long id = saved.getId();
    resumeExecutor.execute(() -> processor.process(id));
    return detail(userId, id);
  }

  @Transactional(readOnly = true)
  public ResumeDetail detail(Long userId, Long id) {
    return toDetail(find(userId, id));
  }

  /** 본인 이력서의 경험 목록(seq 순). 남의 이력서는 404. */
  @Transactional(readOnly = true)
  public List<ExperienceView> experiences(Long userId, Long id) {
    Resume resume = find(userId, id);
    if (resume.getStatus() != Resume.Status.PARSED) {
      return List.of(); // 처리 중이거나 실패한 이력서의 경험은 보여주지 않는다
    }
    return experiences.findByResumeId(resume.getId()).stream().map(ExperienceView::of).toList();
  }

  @Transactional(readOnly = true)
  public List<ResumeSummary> list(Long userId) {
    return resumes.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(ResumeSummary::of)
        .toList();
  }

  /**
   * 구조화 결과 수정. 경험 임베딩(외부 API)은 트랜잭션 밖에서 먼저 계산하고, 결과 저장과 경험 교체는 한 트랜잭션으로 묶는다. 임베딩이 실패하면 아무것도 바뀌지 않고
   * 503(AI_UNAVAILABLE)으로 끝난다(사용자는 다시 저장).
   */
  public ResumeDetail updateParsed(Long userId, Long id, ParsedResume parsed) {
    ExperienceSync.Prepared prepared = experienceSync.prepare(parsed);
    return tx.execute(
        status -> {
          // 같은 이력서를 동시에 고치면 여기서 순서대로 기다린다(경험 교체가 겹치지 않게)
          Resume resume = resumes.lockByIdAndUserId(id, userId).orElseThrow(ApiException::notFound);
          // 파싱 전 수정 금지 규칙은 도메인(Resume.updateParsed)이 지킨다 → 409 RESUME_NOT_READY
          resume.updateParsed(json.writeValueAsString(parsed));
          experienceSync.save(resume.getId(), prepared);
          return toDetail(resume);
        });
  }

  private Resume find(Long userId, Long id) {
    return resumes.findByIdAndUserId(id, userId).orElseThrow(ApiException::notFound);
  }

  private ResumeDetail toDetail(Resume r) {
    ParsedResume parsed =
        r.getParsedJson() == null ? null : json.readValue(r.getParsedJson(), ParsedResume.class);
    return ResumeDetail.of(r, parsed);
  }
}

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

  public ResumeService(
      ResumeRepository resumes,
      FileStorage storage,
      ResumeProcessor processor,
      TaskExecutor resumeExecutor,
      ObjectMapper json,
      ExperienceSync experienceSync,
      ResumeExperienceRepository experiences) {
    this.resumes = resumes;
    this.storage = storage;
    this.processor = processor;
    this.resumeExecutor = resumeExecutor;
    this.json = json;
    this.experienceSync = experienceSync;
    this.experiences = experiences;
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
    return experiences.findByResumeId(resume.getId()).stream().map(ExperienceView::of).toList();
  }

  @Transactional(readOnly = true)
  public List<ResumeSummary> list(Long userId) {
    return resumes.findByUserIdOrderByCreatedAtDesc(userId).stream()
        .map(ResumeSummary::of)
        .toList();
  }

  @Transactional
  public ResumeDetail updateParsed(Long userId, Long id, ParsedResume parsed) {
    Resume resume = find(userId, id);
    // 파싱 전 수정 금지 규칙은 도메인(Resume.updateParsed)이 지킨다 → 409 RESUME_NOT_READY
    resume.updateParsed(json.writeValueAsString(parsed));
    // 고친 내용(기술 등)을 경험에도 반영
    experienceSync.sync(resume.getId(), parsed);
    return toDetail(resume);
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

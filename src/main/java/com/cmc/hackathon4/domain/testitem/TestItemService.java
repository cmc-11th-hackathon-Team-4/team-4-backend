package com.cmc.hackathon4.domain.testitem;

import java.util.List;

import com.cmc.hackathon4.global.exception.BusinessException;
import com.cmc.hackathon4.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TestItemService {

	private final TestItemRepository testItemRepository;

	@Transactional
	public TestItemResponse create(TestItemCreateRequest request) {
		TestItem item = testItemRepository.save(new TestItem(request.title(), request.content()));
		return TestItemResponse.from(item);
	}

	// shortcut: 페이지네이션 없이 전체 조회, 임시 테스트용이라 데이터가 적다는 전제
	public List<TestItemResponse> findAll() {
		return testItemRepository.findAll().stream().map(TestItemResponse::from).toList();
	}

	public TestItemResponse findById(Long id) {
		return TestItemResponse.from(getItem(id));
	}

	@Transactional
	public TestItemResponse update(Long id, TestItemUpdateRequest request) {
		TestItem item = getItem(id);
		item.update(request.title(), request.content());
		testItemRepository.flush(); // updatedAt 이 응답에 반영되도록 먼저 반영한다
		return TestItemResponse.from(item);
	}

	@Transactional
	public void delete(Long id) {
		testItemRepository.delete(getItem(id));
	}

	private TestItem getItem(Long id) {
		return testItemRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.TEST_ITEM_NOT_FOUND));
	}

}

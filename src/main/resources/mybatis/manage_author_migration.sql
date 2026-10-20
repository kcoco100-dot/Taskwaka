-- [신규] manage 프로젝트/업무 작성자 기록: 최초 적용 시 Oracle에서 1회 실행
-- 기존 projects/tasks 작성자는 원본 스키마로 식별 불가: 추측하여 백필하지 말 것.
CREATE TABLE manage_object_authors (
    resource_type VARCHAR2(20) NOT NULL,
    resource_id NUMBER(19) NOT NULL,
    member_id NUMBER(19) NOT NULL,
    CONSTRAINT pk_manage_object_authors PRIMARY KEY (resource_type, resource_id),
    CONSTRAINT ck_manage_object_type CHECK (resource_type IN ('PROJECT', 'TASK')),
    CONSTRAINT fk_manage_object_author_member FOREIGN KEY (member_id) REFERENCES members (member_id)
);
-- 기존 데이터의 실제 작성자가 확인된 경우에만 아래처럼 수동 작성 (예시, 번호 교체):
-- INSERT INTO manage_object_authors(resource_type, resource_id, member_id) VALUES ('PROJECT', 123, 456);
-- INSERT INTO manage_object_authors(resource_type, resource_id, member_id) VALUES ('TASK', 789, 456);

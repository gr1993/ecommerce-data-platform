# frontend-service
[이전 프로젝트](https://github.com/gr1993/ecommerce-msa/tree/main/service/frontend-service)에서 관리자 대시보드와 정산 기능 일부를 발췌해 구성한 간단한 React 프로젝트이다.

### 프로젝트 구조
```
[src]
  └── api : REST API 호출 로직
  └── components : 페이지에 사용되는 컴포넌트 정의
  └── config : 환경변수 등 설정 관련 정의
  └── hooks : 커스텀 훅 정의
  └── pages : URL 단위로 제작되는 페이지
  └── stores : Zustand 전역 상태 관리 스토어
  └── utils : 유틸리티 함수
```

### 프론트 기술
* Vite 7.2.4
* typescript
* react 19.2.5
* react-router-dom 7.10.1
* zustand 5.0.2 : 전역 상태 관리 라이브러리
* react-quill-new 3.6.0 : 에디터 라이브러리로 기존 react-quill은 React 19에서 ReactDOM.findDOMNode가 제거되었기 때문에 사용할 수 없어서 react-quill-new로 사용
* antd 6.1.0 : Ant Design 라이브러리로 Ant Design Table을 사용하기 위해 추가
* @ant-design/charts : 차트 라이브러리


### 관리자 메뉴
* 대시보드
    * 대시보드
* 정산 관리
    * 정산 관리
    * 매출 통계
/**
 * PhysicsUtil 클래스는 물리 계산에 도움을 주는 유틸리티 함수 모음입니다.
 * (객체 생성 방지: 생성자는 private)
 */
public final class PhysicsUtil {
    private PhysicsUtil() {
    }

    /**
     * 점이 다각형(Polygon) 내부에 있는지 검사합니다.
     *
     * <b>알고리즘 설명:</b>
     * - 'Ray Casting(레이 캐스팅) 알고리즘'을 사용합니다.
     * - 점(p)에서 오른쪽으로 반직선을 그었을 때,
     *   다각형의 변과 교차하는 횟수를 셉니다.
     * - 홀수 번 교차하면 내부, 짝수 번 교차하면 외부로 판단합니다.
     *
     * 참고(알고리즘 상세):
     * - https://en.wikipedia.org/wiki/Point_in_polygon#Ray_casting_algorithm
     *
     * @param p    테스트할 점의 좌표 (Vector2D)
     * @param poly 다각형 꼭짓점 배열 (Vector2D[])
     * @return 점이 다각형 내부에 있으면 true, 아니면 false
     */
    public static boolean pointInPolygon(final Vector2D p, final Vector2D[] poly) {
        boolean isInside = false;

        // 다각형의 마지막 꼭짓점부터 시작
        int j = poly.length - 1;
        for (int i = 0; i < poly.length; ++i) {
            /*
             * 아래 조건의 의미:
             * 1. 점 p의 y좌표가 poly[i], poly[j]의 y 사이에 있는지 확인
             * 2. 점 p에서 오른쪽으로 반직선을 그었을 때, poly[i]-poly[j] 변과 교차하는지 확인
             * 3. 교차하면 isInside 값을 반전 (홀짝성 판정)
             */
            if (((poly[i].getY() > p.getY()) != (poly[j].getY() > p.getY())) &&
                    (p.getX() < (poly[j].getX() - poly[i].getX()) * (p.getY() - poly[i].getY()) / (poly[j].getY() - poly[i].getY()) + poly[i].getX())) {
                isInside = !isInside;
            }

            j = i;
        }

        return isInside;
    }
}

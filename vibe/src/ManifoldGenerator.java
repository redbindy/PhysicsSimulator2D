import java.util.ArrayList;
import java.util.List;

/**
 * ManifoldGenerator 클래스는 두 물체(다각형)의 충돌 여부를 판정하고,
 * 충돌 정보(Manifold)를 생성하는 역할을 합니다.
 *
 * 여기서는 'Separating Axis Theorem(분리축 정리, SAT)'을 활용해
 * 다각형끼리의 충돌(겹침 여부)와 충돌 세부 정보를 계산합니다.
 *
 * 참고:
 * - SAT(Separating Axis Theorem): https://en.wikipedia.org/wiki/Hyperplane_separation_theorem
 * - SAT 설명 및 예제: https://www.jeffreythompson.org/collision-detection/poly-poly.php
 * - 충돌 처리와 접점 탐색: https://learnopengl.com/In-Practice/2D-Game/Collisions/Collision-resolution
 */
public final class ManifoldGenerator {
    /**
     * 생성자를 private으로 두어, 객체 생성을 방지합니다.
     * (모든 메서드는 static)
     */
    private ManifoldGenerator() {
    }

    /**
     * 두 RigidBody가 충돌하는지 검사하고,
     * 충돌하면 Manifold 정보를 생성해서 반환합니다.
     * (현재는 다각형-다각형만 지원)
     */
    public static Manifold collide(final RigidBody a, final RigidBody b) {
        // 실제 구현은 collidePolyPoly로 위임합니다.
        return collidePolyPoly(a, b);
    }

    /**
     * 두 다각형(각각의 RigidBody)의 충돌 여부와 충돌 정보를 계산합니다.
     *
     * 충돌 원리는 다음과 같습니다:
     * 1. 두 다각형의 모든 변(Edge)에 대해, 수직 방향(=법선 벡터, 축)을 찾음
     * 2. 각 축에 두 도형을 '투영(Projection)'해 겹치는지(Overlap) 확인
     * 3. 모든 축에서 한 번이라도 겹치지 않으면 충돌 없음 (SAT 이론)
     * 4. 모든 축에서 겹치면, 겹침이 가장 작은 축(최소 penetration, 충돌 법선)을 기록
     * 5. 실제로 맞닿은 점(contacts)을 찾아서 Manifold로 반환
     *
     * SAT(분리축 정리) 참고:
     * https://www.jeffreythompson.org/collision-detection/poly-poly.php
     */
    private static Manifold collidePolyPoly(final RigidBody A, final RigidBody B) {
        final Vector2D[] vertsA = A.getVertices();
        final Vector2D[] vertsB = B.getVertices();

        // 두 다각형이 겹치는(=충돌) 최소 거리(=관통 깊이)
        double minOverlap = Double.POSITIVE_INFINITY;

        // 겹침이 가장 작은 축(=법선 벡터, 충돌 방향)
        Vector2D smallestAxis = null;
        // 축이 뒤집혔는지 기록 (법선 방향 보정에 사용)
        boolean isFliped = false;

        // 두 도형(A, B) 각각의 모든 변(edge)에 대해 반복
        for (int shape = 0; shape < 2; shape++) {
            final Vector2D[] verts;
            if (shape == 0) {
                verts = vertsA;
            } else {
                verts = vertsB;
            }

            for (int i = 0; i < verts.length; i++) {
                final int j = (i + 1) % verts.length;

                // 현재 변(edge) 계산
                final Vector2D edge = verts[j].subtract(verts[i]);
                // 변에 수직인 방향(=법선, 축) 구함
                final Vector2D axis = edge.perpendicular().normalize();

                // 두 도형을 각각 axis(축)에 투영
                final double[] projA = projectOntoAxis(vertsA, axis);
                final double[] projB = projectOntoAxis(vertsB, axis);

                // 두 투영이 얼마나 겹치는지(Overlap) 계산
                final double overlap = Math.min(projA[1], projB[1]) - Math.max(projA[0], projB[0]);

                // 한 번이라도 투영이 겹치지 않으면, 두 도형은 떨어져 있음
                if (overlap < 0) {
                    return null; // 충돌 없음
                }

                // 겹침이 더 작은 축이 있다면, 최소 겹침/축을 갱신
                if (overlap < minOverlap) {
                    minOverlap = overlap;
                    smallestAxis = axis;
                    isFliped = shape == 1;
                }
            }
        }

        // 두 도형 중심을 잇는 벡터 방향 계산
        Vector2D dir = B.getPosition().subtract(A.getPosition()).normalize();

        // 최종 충돌 법선 벡터 방향 결정 (외부로 뻗도록)
        Vector2D normal;
        if (isFliped) {
            normal = smallestAxis.multiply(-1);
        } else {
            normal = smallestAxis;
        }

        // 만약 중심점 방향과 법선 방향이 반대라면, 법선 방향 반전
        if (dir.dot(normal) < 0) {
            normal = normal.multiply(-1);
        }

        // 실제로 맞닿은 점(contacts)들을 찾음
        final List<Vector2D> contacts = new ArrayList<>();
        // A의 꼭짓점이 B 안에 있으면 contact로 추가
        for (final Vector2D va : vertsA) {
            if (PhysicsUtil.pointInPolygon(va, vertsB)) {
                contacts.add(va);
            }
        }
        // B의 꼭짓점이 A 안에 있으면 contact로 추가
        for (final Vector2D vb : vertsB) {
            if (PhysicsUtil.pointInPolygon(vb, vertsA)) {
                contacts.add(vb);
            }
        }

        // 만약 접점이 전혀 없을 때는, 두 도형에서 가장 가까운 두 점의 중점을 사용
        if (contacts.isEmpty()) {
            final Vector2D cp = closestPoint(vertsA, vertsB);
            if (cp != null) {
                contacts.add(cp);
            }
        }

        // 그래도 없으면 충돌 무효
        if (contacts.isEmpty()) {
            return null;
        }

        // 최종적으로 Manifold 생성 및 반환
        return new Manifold(A, B, normal, minOverlap, contacts);
    }

    /**
     * 주어진 다각형 꼭짓점 배열을 주어진 축(axis)에 투영(Projection)합니다.
     *
     * 투영: 벡터 내적(dot product)으로 각 꼭짓점을 1차원(축 방향) 값으로 변환
     * 반환: [최솟값, 최댓값] 배열 (투영선 위에서의 구간)
     *
     * 참고:
     * - 벡터 투영(Projection): https://en.wikipedia.org/wiki/Vector_projection
     * - SAT 투영 설명: https://www.jeffreythompson.org/collision-detection/poly-poly.php
     */
    private static double[] projectOntoAxis(final Vector2D[] verts, final Vector2D axis) {
        double min = verts[0].dot(axis);
        double max = min;

        for (int i = 1; i < verts.length; i++) {
            final double p = verts[i].dot(axis);

            if (p < min) {
                min = p;
            } else if (p > max) {
                max = p;
            }
        }

        // [투영선의 최소값, 최대값] 반환
        return new double[]{min, max};
    }

    /**
     * 두 다각형의 꼭짓점들 중, 서로 가장 가까운 두 점을 찾아서
     * 그 중간점(average)을 반환합니다.
     *
     * 접점이 완전히 겹치지 않을 때, 가장 가까운 위치를 추정 contact로 사용
     *
     * 참고:
     * - 최근접 점 계산: https://math.stackexchange.com/questions/2213165/how-to-find-the-closest-point-between-two-convex-polygons
     */
    private static Vector2D closestPoint(final Vector2D[] vertsA, final Vector2D[] vertsB) {
        double minDist = Double.POSITIVE_INFINITY;

        Vector2D minP = null;
        for (final Vector2D a : vertsA) {
            for (final Vector2D b : vertsB) {
                // 두 점 사이 거리 계산
                final double d = a.subtract(b).length();
                if (d < minDist) {
                    minDist = d;
                    // 두 점의 중점
                    minP = Vector2D.average(a, b);
                }
            }
        }

        return minP;
    }
}
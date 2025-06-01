/**
 * 2차원 벡터를 표현하는 클래스.
 * - 모든 연산(덧셈, 뺄셈, 스칼라곱, 내적, 외적, 회전 등) 불변(immutable) 객체로 반환
 *
 * 참고:
 * - 벡터의 기초: https://en.wikipedia.org/wiki/Euclidean_vector
 * - 물리 및 그래픽스에서 자주 사용됨
 */
public final class Vector2D {
    // x좌표, y좌표(불변)
    private final double x;
    private final double y;

    /**
     * (생성자) x, y값을 받아 새로운 2D 벡터 생성
     */
    public Vector2D(final double x, final double y) {
        this.x = x;
        this.y = y;
    }

    // (getter) x좌표 반환
    public double getX() {
        return this.x;
    }

    // (getter) y좌표 반환
    public double getY() {
        return this.y;
    }

    /**
     * 두 벡터의 합(덧셈).
     * 예: (1,2) + (3,4) = (4,6)
     */
    public Vector2D add(final Vector2D v) {
        return new Vector2D(this.x + v.x, this.y + v.y);
    }

    /**
     * 두 벡터의 차(뺄셈).
     * 예: (3,4) - (1,2) = (2,2)
     */
    public Vector2D subtract(final Vector2D v) {
        return new Vector2D(this.x - v.x, this.y - v.y);
    }

    /**
     * 스칼라(실수)값과 곱셈.
     * 예: (1,2) * 3 = (3,6)
     */
    public Vector2D multiply(final double s) {
        return new Vector2D(this.x * s, this.y * s);
    }

    /**
     * 내적(dot product, scalar product)
     * - 두 벡터가 이루는 각의 코사인(방향성) 관련 값
     * - 공식: x1*x2 + y1*y2
     * - 예: (1,0)·(0,1)=0 (수직), (1,0)·(1,0)=1 (동일 방향)
     * - 참고: https://en.wikipedia.org/wiki/Dot_product
     */
    public double dot(final Vector2D v) {
        return this.x * v.x + this.y * v.y;
    }

    /**
     * 외적(cross product, 2D에서 '스칼라'값)
     * - 두 벡터가 이루는 평행사변형의 면적, 방향(회전 방향) 정보 포함
     * - 공식: x1*y2 - y1*x2
     * - 참고: https://en.wikipedia.org/wiki/Cross_product#Cross_product_in_two_dimensions
     */
    public double cross(final Vector2D v) {
        return this.x * v.y - this.y * v.x;
    }

    /**
     * 벡터의 크기(길이, 노름, norm, magnitude)
     * - 피타고라스 공식: sqrt(x^2 + y^2)
     * - 예: (3,4)의 길이 = 5
     */
    public double length() {
        return Math.sqrt(this.x * this.x + this.y * this.y);
    }

    /**
     * 단위 벡터(방향만 유지, 크기는 1)
     * - 길이가 0이면 (0,0) 반환
     * - 참고: 단위 벡터로 방향만 뽑아낼 때 사용
     */
    public Vector2D normalize() {
        final double len = this.length();

        if (len == 0) {
            return new Vector2D(0, 0);
        }

        return new Vector2D(this.x / len, this.y / len);
    }

    /**
     * 벡터를 원점 기준으로 angle(라디안)만큼 회전
     * - 회전 행렬 공식 사용
     * - 참고: https://en.wikipedia.org/wiki/Rotation_matrix
     */
    public Vector2D rotate(final double angle) {
        final double c = Math.cos(angle);
        final double s = Math.sin(angle);

        return new Vector2D(c * this.x - s * this.y, s * this.x + c * this.y);
    }

    /**
     * 벡터에 수직인 벡터 반환 (90도 회전, (x,y)→(-y,x))
     * - 2D 그래픽스, 물리 엔진 등에서 접선, 법선 구할 때 자주 사용
     */
    public Vector2D perpendicular() {
        return new Vector2D(-this.y, this.x);
    }

    /**
     * 두 벡터의 평균(중간점) 반환
     * - 예: (0,0), (2,2)의 평균 = (1,1)
     */
    public static Vector2D average(final Vector2D a, final Vector2D b) {
        return new Vector2D((a.x + b.x) / 2, (a.y + b.y) / 2);
    }
}

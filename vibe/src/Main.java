public final class Main {
    public static void main(final String[] args) {
        final World world = new World();
        final UI ui = new UI(world);
        ui.show();
    }
}

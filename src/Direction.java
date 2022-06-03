public enum Direction
{
    DOWN { @Override public Direction opposite() {return UP;}},
    LEFT { @Override public Direction opposite() {return RIGHT;}},
    UP { @Override public Direction opposite() {return DOWN;}},
    RIGHT { @Override public Direction opposite() {return LEFT;}};

    abstract public Direction opposite();

    public Direction changeDirection(Direction direction)
    {
        return direction.opposite() == this ? this : direction;
    }
}

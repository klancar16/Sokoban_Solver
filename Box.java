package sokoban;

public class Box {
	public Coordinate coor;
	
	public boolean inPos;
	
	public Box(byte x, byte y, boolean inPos) {
		coor = new Coordinate(x, y);
		this.inPos = inPos;
	}
	
	public void move(byte newX, byte newY) {
		this.coor.x = newX;
		this.coor.y = newY;
	}
}

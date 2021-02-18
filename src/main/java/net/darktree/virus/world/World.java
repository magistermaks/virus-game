package net.darktree.virus.world;

import net.darktree.virus.Const;
import net.darktree.virus.Main;
import net.darktree.virus.cell.Cell;
import net.darktree.virus.cell.CellType;
import net.darktree.virus.gui.graph.GraphFrame;
import net.darktree.virus.particle.Particle;
import net.darktree.virus.particle.ParticleContainer;
import net.darktree.virus.particle.ParticleType;
import net.darktree.virus.util.Vec2f;

import java.util.ArrayList;

public class World {

    public Cell[][] cells;
    public int size;

    public ParticleContainer pc = new ParticleContainer();

    public ArrayList<Particle> queue = new ArrayList<>();
    public int initialCount = 0;
    public int aliveCount;
    public int deadCount = 0;
    public int shellCount = 0;
    public int infectedCount = 0;
    public int lastEditFrame = 0;
    public int totalFoodCount = 0;
    public int totalWasteCount = 0;
    public int totalUGOCount = 0;

    public World() {

        this.size = Const.WORLD_SIZE;
        cells = new Cell[ size ][ size ];
        CellType[] types = CellType.values();

        for( int y = 0; y < size; y++ ) {
            for( int x = 0; x < size; x++ ) {

                CellType type = types[Const.WORLD_DATA[x][y]];

                if( type == CellType.Empty ) {
                    cells[y][x] = null;
                    continue;
                }

                Cell cell = new Cell( x, y, type, 0, 1, Const.DEFAULT_CELL_GENOME );
                cells[y][x] = cell;

                if( cell.type == CellType.Normal ) initialCount ++;
                if( cell.type == CellType.Shell ) shellCount ++;

            }
        }

        aliveCount = initialCount;

    }

    public void tick() {

        if( Main.applet.frameCount % Const.GRAPH_UPDATE_PERIOD == 0 ) {
            Main.applet.graph.append( new GraphFrame(
                    pc.get(ParticleType.WASTE).size(),
                    pc.get(ParticleType.UGO).size(),
                    aliveCount + shellCount) );
        }

        pc.tick( ParticleType.FOOD );
        pc.tick( ParticleType.WASTE );
        pc.tick( ParticleType.UGO );

        for( int y = 0; y < size; y++ ) {
            for( int x = 0; x < size; x++ ) {
                Cell c = cells[y][x];
                if( c != null ) {
                    c.tick();
                    if( c.type == CellType.Empty ) cells[y][x] = null;
                }
            }
        }

        pc.randomTick();
        pc.add( queue );
    }

    public void updateParticleCount() {

        int count = 0;

        while(pc.foods.size() + count < Const.MAX_FOOD) {

            int x = -1, y = -1;

            for( int i = 0; i < 16 && (x == -1 || cells[x][y] != null); i ++ ) {
                x = (int) Main.applet.random(0, Const.WORLD_SIZE);
                y = (int) Main.applet.random(0, Const.WORLD_SIZE);
            }

            Vec2f pos = new Vec2f(
                    x + Main.applet.random(0.3f, 0.7f),
                    y + Main.applet.random(0.3f, 0.7f)
            );

            Particle food = new Particle(pos, ParticleType.FOOD, Main.applet.frameCount);
            addParticle( food );
            count ++;

        }

    }

    public void addParticle( Particle p ) {
        p.addToCellList();
        queue.add( p );
    }

    public void setCellAt( int x, int y, Cell c ) {
        if( cells[y][x] != null ) cells[y][x].die(true);
        cells[y][x] = c;
    }

    public boolean isCellValid( int x, int y ) {
        return !(x < 0 || x >= size || y < 0 || y >= size);
    }

    public boolean isCellValid( double x, double y ) {
        return !(x < 0 || x >= size || y < 0 || y >= size);
    }

    public Cell getCellAt(double x, double y ) {
        int ix = ((int) x + size) % size;
        int iy = ((int) y + size) % size;

        if(ix < 0 || ix >= size || iy < 0 || iy >= size) {
            return null;
        }

        return cells[iy][ix];
    }

    public CellType getCellTypeAt(double x, double y ) {
        Cell c = getCellAt( x, y );
        if( c != null ) {
            return c.type;
        }

        return CellType.Empty;
    }

    public Cell getCellAtUnscaled(double x, double y ) {
        int ix = (int) x;
        int iy = (int) y;

        if(ix < 0 || ix >= size || iy < 0 || iy >= size) {
            return null;
        }

        return cells[iy][ix];
    }

    public void draw() {
        // draw all cells
        for( int y = 0; y < size; y++ ) {
            for( int x = 0; x < size; x++ ) {
                Cell cell = cells[y][x];
                if( cell != null ) cell.draw();
            }
        }

        // draw particles
        pc.draw();
    }

}

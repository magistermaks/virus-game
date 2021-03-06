package net.darktree.virus.cell;

import net.darktree.virus.Const;
import net.darktree.virus.Main;
import net.darktree.virus.codon.Codon;
import net.darktree.virus.genome.CellGenome;
import net.darktree.virus.gui.Screen;
import net.darktree.virus.particle.FoodParticle;
import net.darktree.virus.particle.Particle;
import net.darktree.virus.particle.VirusParticle;
import net.darktree.virus.particle.WasteParticle;
import net.darktree.virus.util.Helpers;
import net.darktree.virus.util.Utils;
import net.darktree.virus.util.Vec2f;

import java.util.ArrayList;

public class NormalCell extends ShellCell implements GenomeCell {

    public CellGenome genome;
    public float geneTimer;
    public boolean tampered = false;
    private final Laser laser = new Laser();
    public String memory = "";

    public NormalCell(int x, int y, String dna) {
        super(x, y);
        genome = new CellGenome(dna);
        genome.selected = (int) (Math.random() * genome.codons.size());
        geneTimer = (float) (Math.random() * Const.GENE_TICK_TIME);
    }

    public NormalCell(int ex, int ey, ArrayList<Codon> codons) {
        super(ex, ey);
        genome = new CellGenome(codons);
        genome.selected = (int) (Math.random() * genome.codons.size());
        geneTimer = (float) (Math.random() * Const.GENE_TICK_TIME);
    }

    @Override
    public CellGenome getGenome() {
        return genome;
    }

    @Override
    public String getCellName(){
        return "Cell at (" + x + ", " + y + ")";
    }

    @Override
    protected void drawCell(Screen screen) {
        drawCellBackground( (tampered && Main.showTampered) ? Const.COLOR_CELL_TAMPERED : Const.COLOR_CELL_BACK );

        push();
        translate(Const.BIG_FACTOR * 0.5f, Const.BIG_FACTOR * 0.5f);

        if(screen.camS > Const.DETAIL_THRESHOLD) {
            genome.drawInterpreter(geneTimer);
            genome.drawCodons(Const.CODON_DIST);
        }

        drawEnergy();
        genome.drawHand();
        pop();
    }

    @Override
    protected void unscaledDraw(Screen screen) {
        laser.draw(screen, getHandPos());
    }

    public String getMemory() {
        return memory.length() == 0 ? "empty" : "\"" + memory + "\"";
    }

    public void hurtWall(double multi){
        wall -= Const.WALL_DAMAGE * multi;
        if(wall <= 0) die(false);
    }

    public boolean isHandInwards() {
        return genome.inwards;
    }

    public boolean tamper() {
        boolean old = tampered;
        tampered = true;
        return old;
    }

    public void useEnergy() {
        useEnergy( Const.GENE_TICK_ENERGY );
    }

    public void useEnergy( float amount ){
        energy = Math.max(0, energy - amount);
    }

    public void laserWall(){
        laser.targetWall(x, y);
    }

    public Vec2f getHandPos(){
        float r = Const.HAND_DIST + ( genome.inwards ? -Const.HAND_LEN : Const.HAND_LEN ) ;
        return genome.getCodonPos( genome.pointed, r, x, y );
    }

    public void eat(Particle p){
        if(p instanceof FoodParticle){
            Particle waste = new WasteParticle(p.pos, Helpers.combineVelocity( p.velocity, Helpers.getRandomVelocity() ), -99999);
            laser.targetParticle(waste);
            Main.applet.world.addParticle(waste);
            p.removeParticle(this);
            giveEnergy();
        }else{
            laser.targetParticle(p);
        }
    }

    public void readToMemory(int start, int end) {
        memory = "";
        laser.reset();

        StringBuilder dna = new StringBuilder(memory);

        for(int pos = start; pos <= end; pos++){
            int index = Helpers.loopItInt( genome.pointed + pos, genome.codons.size() );
            dna.append( genome.codons.get(index).asDNA() ).append('-');
            laser.addTargetPos( genome.getCodonPos(index, Const.CODON_DIST, x, y) );
        }

        memory =  dna.length() != 0 ? dna.substring(0, dna.length() - 1) : "";
    }

    public void writeFromMemory(int start, int end){
        if(memory.length() != 0) {
            if( genome.inwards ){
                writeInwards(start, end);
            }else{
                writeOutwards();
            }
        }
    }

    private void writeOutwards() {
        VirusParticle virus = new VirusParticle(getHandPos(), memory);
        virus.mutate();
        Main.applet.world.addParticle(virus);
        laser.targetParticle(virus);

        String[] memoryParts = memory.split("-");
        for(int i = 0; i < memoryParts.length; i++){
            useEnergy();
        }
    }

    private void writeInwards(int start, int end){
        laser.reset();
        String[] memoryParts = memory.split("-");
        for(int pos = start; pos <= end; pos++){
            int index = Helpers.loopItInt(genome.pointed +pos,genome.codons.size());
            if(pos-start < memoryParts.length){
                String memoryPart = memoryParts[pos-start];
                genome.codons.set(index, new Codon( memoryPart ));
                laser.addTargetPos( genome.getCodonPos(index, Const.CODON_DIST, x, y) );
            }
            useEnergy();
        }
    }

    public void pushOut(Particle particle){
        int[][] dire = {{0,1}, {0,-1}, {1,0}, {-1,0}};
        int chosen = -1;

        for( int i = 0; i < 16 && chosen == -1; i ++ ) {
            int c = Utils.random(0, 4);

            if( Main.applet.world.isCellValid( x + dire[c][0], y + dire[c][1] ) && Main.applet.world.getCellAt( y + dire[c][1], x + dire[c][0] ) == null ) {
                chosen = c;
            }
        }

        // failed to find suitable push direction
        if( chosen == -1 ) return;

        Vec2f old = particle.pos.copy();
        float m1 = dire[chosen][0], m2 = dire[chosen][1];

        if(m1 != 0){
            particle.pos.x = Utils.ceilOrFloor(particle.pos.x, m1) + EPSILON * m1;
            particle.velocity.x = Main.abs(particle.velocity.x) * m1;
        }

        if(m2 != 0){
            particle.pos.y = Utils.ceilOrFloor(particle.pos.y, m2) + EPSILON * m2;
            particle.velocity.y = Main.abs(particle.velocity.y) * m2;
        }

        particle.alignWithWorld();

        ContainerCell pCell = Main.applet.world.getCellAt( (int) old.x, (int) old.y, ContainerCell.class );
        if( pCell != null ) pCell.removeParticle(particle);

        ContainerCell nCell = Main.applet.world.getCellAt( (int) particle.pos.x, (int) particle.pos.y, ContainerCell.class );
        if( nCell != null ) nCell.addParticle(particle);

        laser.targetParticle(particle);
    }

    @Override
    public void tick(){
        if(energy > 0){
            float oldGT = geneTimer;
            geneTimer -= Const.PLAY_SPEED;

            if(geneTimer <= Const.GENE_TICK_TIME / 2.0f && oldGT > Const.GENE_TICK_TIME / 2.0f){
                Codon codon = genome.getSelected();
                if( codon != null ) {
                    genome.hurtCodons(this);
                    codon.tick(this);
                }
            }

            if(geneTimer <= 0){
                geneTimer += Const.GENE_TICK_TIME;
                genome.next();
            }
        }

        genome.update();
    }

    @Override
    public void die(boolean silent) {
        if( !silent ) {
            for(int i = 0; i < genome.codons.size(); i++){
                Particle waste = new WasteParticle( genome.getCodonPos(i, Const.CODON_DIST, x, y), -99999 );
                Main.applet.world.addParticle( waste );
            }
        }

        Main.applet.world.aliveCount --;
        super.die(silent);
    }

    @Override
    public CellType getType() {
        return CellType.Normal;
    }

}

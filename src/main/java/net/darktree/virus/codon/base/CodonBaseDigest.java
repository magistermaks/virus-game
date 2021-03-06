package net.darktree.virus.codon.base;

import net.darktree.virus.Const;
import net.darktree.virus.cell.NormalCell;
import net.darktree.virus.codon.CodonArgs;
import net.darktree.virus.codon.CodonMetaInfo;
import net.darktree.virus.codon.arg.CodonArg;
import net.darktree.virus.particle.Particle;
import net.darktree.virus.particle.ParticleType;

public class CodonBaseDigest extends CodonBase {

    public CodonBaseDigest( int code, CodonMetaInfo info ) {
        super( code, new CodonArg[] { CodonArgs.NONE, CodonArgs.FOOD, CodonArgs.WASTE, CodonArgs.WALL }, info );
    }

    @Override
    public void tick(NormalCell cell, CodonArg arg ) {
        if( !cell.isHandInwards() ) {
            if(arg == CodonArgs.WALL){
                cell.hurtWall(25);
                cell.laserWall();
                cell.useEnergy( (1 - cell.energy) * Const.E_RECIPROCAL * -0.2f );
            }else{
                Particle p = null;
                cell.useEnergy();

                if(arg == CodonArgs.FOOD) {
                    p = cell.selectParticle( ParticleType.FOOD );
                }else if(arg == CodonArgs.WASTE) {
                    p = cell.selectParticle( ParticleType.WASTE );
                }

                if(p != null) cell.eat(p);
            }
        }
    }

}

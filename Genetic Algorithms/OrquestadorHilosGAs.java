import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class OrquestadorHilosGAs
{
    CyclicBarrier barrera;

    int individuos, dimension,iteraciones;
    int intercambio, nGAs, frecuencia;

    Function<int[], Integer> fitness;

    GAhilo[] v;
    int bestValue = Integer.MAX_VALUE;
    int[] bestSol;

    public class ArgGAs{
        final int Seleccion, CrossOver, Mutacion, Reemplazo;
        final float ProbCross,ProbMut;

        public ArgGAs(int s, int c, int m, int r, float pc, float pm)
        {
            this.Seleccion = s;
            this.CrossOver = c;
            this.Mutacion = m;
            this.Reemplazo = r;
            this.ProbCross = pc;
            this.ProbMut = pm;
        }
    }

    public class res{
        int[] bestSol;
        int bestValue;

        public res(int[] sol, int value)
        {
            this.bestSol = Arrays.copyOf(sol, sol.length);
            this.bestValue = value;
        }
    }

    public OrquestadorHilosGAs(int d, int ind, int inter, Function<int[],Integer> f,ArgGAs[] argumentos,int iter, int frec)
    {
        this.individuos = ind;
        this.dimension = d;
        this.intercambio = inter;
        this.nGAs = argumentos.length;
        this.fitness = f;
        this.iteraciones = iter;
        this.frecuencia = frec;

        this.barrera = new CyclicBarrier(argumentos.length+1);

        if(inter*this.nGAs>ind)
            throw new IllegalStateException("No se pueden hacer tantos cambios con "+this.nGAs+" para "+ind+" individuos");

        this.v= new GAhilo[this.nGAs];

        for(int i=0;i<this.nGAs;i++)
            this.v[i] = new GAhilo(this.individuos,this.dimension,argumentos[i].Seleccion,argumentos[i].CrossOver,argumentos[i].Mutacion,
                            argumentos[i].Reemplazo,argumentos[i].ProbCross,argumentos[i].ProbMut,this.fitness,this.barrera,this.frecuencia);
        
    }

    public res ejecutar()
    {
        int[][] trozo = new int[this.nGAs*this.intercambio][this.dimension];

        ExecutorService pool = Executors.newFixedThreadPool(this.v.length);

        for(int i=0;i<this.iteraciones;i++)
        {
            for(int j=0;j<this.nGAs;j++)
                pool.submit(this.v[j]);

            try {
                this.barrera.await();
            } catch (Exception e) {
            }

            CrearTrozo(trozo);

            ActualizarMejores();
            
            MezclarTrozos(trozo);
            

        }

        return new res(this.bestSol,this.bestValue);
    }

    private void ActualizarMejores()
    {
        for(int z=0;z<this.nGAs;z++)
        {
            if(this.v[z].bestValue < this.bestValue)
            {
                this.bestValue = this.v[z].bestValue;
                this.bestSol = Arrays.copyOf(this.v[z].bestSol, this.dimension);
            }
        }

    }

    private void MezclarTrozos(int[][] trozo)
    {
        for(int z=0;z<this.nGAs;z++)
                for(int i2=0;i2<trozo.length;i2++)
                    this.v[z].poblacion[this.individuos-i2-1] = Arrays.copyOf(trozo[i2], this.dimension);
    }

    private void CrearTrozo(int[][] trozo)
    {
        for(int j=0;j<this.nGAs*this.intercambio;j++)
                trozo[j] = Arrays.copyOf(this.v[j/this.intercambio].poblacion[j%this.intercambio], this.dimension);
    }
}

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Function;


public class GAhilo implements Runnable
{
    CyclicBarrier barrera;
    int iteraciones;

    final int dimension, individuos;
    public int[][] poblacion;
    final int Seleccion, CrossOver, Mutacion, Reemplazo;
    final float ProbCross,ProbMut;

    public int bestValue;
    public int[] bestSol;

    public final int totalSelec=3;
    public final int totalCross = 2;
    public final int totalMut = 3;
    public final int totalReem = 2;

    Function<int[], Integer> fitness;

    public GAhilo(int d, int ind, int s, int c, int m, int r, float pc, float pm,Function<int[],Integer> f,CyclicBarrier b,int iter)
    {
        this.poblacion = new int[ind][d];
        this.dimension = d;
        this.individuos = ind;
        this.Seleccion = s;
        this.CrossOver = c;
        this.Mutacion = m;
        this.Reemplazo = r;
        this.ProbCross = pc;
        this.ProbMut = pm;
        this.fitness = f;
        this.barrera = b;
        this.iteraciones = iter;

        if(this.Seleccion <0 || this.Seleccion > (this.totalSelec-1))
            throw new IllegalArgumentException("No existe el algoritmo de seleccion "+this.Seleccion);

        if(this.CrossOver <0 || this.CrossOver > (this.totalCross-1))
            throw new IllegalArgumentException("No existe el algoritmo de cruze "+this.CrossOver);

        if(this.Mutacion <0 || this.Mutacion > (this.totalMut-1))
            throw new IllegalArgumentException("No existe el algoritmo de mutacion "+this.Mutacion);

        if(this.Reemplazo <0 || this.Reemplazo > (this.totalReem-1))
            throw new IllegalArgumentException("No existe el algoritmo de reemplazo "+this.Reemplazo);

        Random rand = new Random();

        for(int i=0;i<ind;i++)
            for(int j=0;j<d;j++)
                this.poblacion[i][j] = rand.nextInt();
    }

    public void run()
    {
        int[][] PoblacionNueva = new int[this.individuos][this.dimension];

        for(int i=0;i<this.iteraciones;i++)
        {
            PoblacionNueva = Seleccion();
            CrossOver(PoblacionNueva);
            Mutacion(PoblacionNueva);
            Reemplazo(PoblacionNueva);
        }

        try {
            this.barrera.await();
        } catch (Exception e) {
        }
        
    }

    private int[][] Seleccion()
    {
        int[][] PoblacionResultante = new int[this.individuos][this.dimension];
        int[] fit = new int[this.individuos];
        Random rand = new Random();

        for(int i=0;i<this.individuos;i++)
            fit[i] = this.fitness.apply(this.poblacion[i]);

        switch(this.Seleccion)
        {
            case 0: //Torneo
                    int k = 3;
                    int ganador;
                    int[] Contendientes = new int[k];
                    int[] indicesContendientes = new int[k];
                    int index;

                    for(int i=0;i<this.individuos;i++)
                    {
                        for(int j=0;j<k;j++)
                        {
                            index = rand.nextInt(this.individuos);
                            Contendientes[j] = fit[index];
                            indicesContendientes[j] = index;
                        }
                        ganador = min(Contendientes);
                        ganador = indicesContendientes[ganador];
                        PoblacionResultante[i] = Arrays.copyOf(this.poblacion[ganador], this.dimension);
                    }
                    break;
            case 1: //Ruleta
                    int total = Arrays.stream(fit).sum();
                    float[] probAcumuladas = new float[this.individuos];
                    float valorUnidad;
                    float x;
                    int selec;

                    if(total == 0)
                        valorUnidad = 0;
                    else
                        valorUnidad = (float) 1/total;
                        

                    probAcumuladas[0] = fit[0]*valorUnidad;
                    for(int i=1;i<this.individuos;i++)
                    {
                        probAcumuladas[i] = probAcumuladas[i-1]+fit[i]*valorUnidad;
                    }

                    for(int i=0;i<this.individuos;i++)
                    {
                        x = rand.nextFloat();
                        selec = PrimerMayor(probAcumuladas, x);
                        PoblacionResultante[i] = Arrays.copyOf(this.poblacion[selec], this.dimension);
                    }
                    break;                  
            case 2: //Random
                    for(int i=0;i<this.individuos;i++)
                        PoblacionResultante[i] = Arrays.copyOf(this.poblacion[rand.nextInt(this.individuos)], this.dimension);
                    break;
            default:
                    // comprobamos en el constructor que this.Seleccion sea un numero valido
        }

        return PoblacionResultante;
    }

    private void CrossOver(int[][] pob)
    {
        int[][] hijos = new int[2][this.dimension];
        Random rand = new Random();

        switch(this.CrossOver)
        {
            case 0: //PointCrossOver
                int punto;
                for(int i=0;i<this.individuos;i=i+2)
                {
                    if(rand.nextFloat()<=this.ProbCross)
                    {
                        punto = rand.nextInt(this.dimension-1);
                        for(int j=0;j<punto;j++)
                        {
                            hijos[0][j] = pob[i+1][j];
                            hijos[1][j] = pob[i][j];
                        }
                        for(int j=punto;j<this.dimension;j++)
                        {
                            hijos[0][j] = pob[i][j];
                            hijos[1][j] = pob[i+1][j];
                        }

                        pob[i] = Arrays.copyOf(hijos[0], this.dimension);
                        pob[i+1] = Arrays.copyOf(hijos[1], this.dimension);
                    }
                }
                break;
            case 1: //Random
                boolean x;
                for(int i=0;i<this.individuos;i=i+2)
                {
                    if(rand.nextFloat()<=this.ProbCross)
                    {
                        for(int j=0;j<this.dimension;j++)
                        {
                            x = rand.nextBoolean();
                            if(x)
                            {
                                hijos[0][j]=pob[i+1][j];
                                hijos[1][j]=pob[i][j];
                            }
                            else
                            {
                                hijos[0][j]=pob[i][j];
                                hijos[1][j]=pob[i+1][j];
                            }
                        }

                        pob[i] = Arrays.copyOf(hijos[0], this.dimension);
                        pob[i+1] = Arrays.copyOf(hijos[1], this.dimension);
                    }
                }
                break;
            default:
        }

    }

    private void Mutacion(int[][] pob)
    {
        Random rand = new Random();
        switch(this.Mutacion)
        {
            case 0: //sumar 1
                for(int i=0;i<this.individuos;i++)
                    for(int j=0;j<this.dimension;j++)
                        if(rand.nextFloat()<this.ProbMut)
                            pob[i][j]++;
                break;
            case 1: //invertir orden
                for(int i=0;i<this.individuos;i++)
                    if(rand.nextFloat()<=this.ProbMut)
                        invertir(pob[i]);
                break;
            case 2: //permutar
                int a1,a2,aux;
                for(int i=0;i<this.individuos;i++)
                    if(rand.nextFloat()<=this.ProbMut)
                    {
                        a1=rand.nextInt(this.dimension);
                        a2=rand.nextInt(this.dimension);

                        aux = pob[i][a1];
                        pob[i][a1] = pob[i][a2];
                        pob[i][a2] = aux;
                    }
                break;
            default:
        }
    }

    private void Reemplazo(int[][] pob)
    {
        int[][] nuevaPob = new int[this.individuos][this.dimension];
        Integer[] indices = ActualizarMejor(pob);
        
        switch(this.Reemplazo)
        {
            case 0: //elitismo
                for(int i=0;i<this.individuos;i++)
                {
                    if(indices[i]<this.individuos)
                        nuevaPob[i] = Arrays.copyOf(this.poblacion[indices[i]], this.dimension);
                    else
                        nuevaPob[i] = Arrays.copyOf(pob[indices[i]-this.individuos],this.dimension);
                }

                for(int i=0;i<this.individuos;i++)
                    this.poblacion[i] = Arrays.copyOf(nuevaPob[i], this.dimension);
                break;

            case 1: //generacional
                for(int i=0;i<this.individuos;i++)
                    this.poblacion[i] = Arrays.copyOf(pob[i], this.dimension);
                break;
            default:
        }

    }

    private int min(int[] v)
    {
        int min = Integer.MAX_VALUE;
        int index=0;

        for(int i=0;i<v.length;i++)
        {
            if(v[i] < min)
            {
                min = v[i];
                index = i;
            }
        }

        return index;
    }

    private int PrimerMayor(float[] v, float x)
    {
        int res=-1;
        boolean cmp = false;

        while(res<v.length-1 && !cmp)
        {
            res++;
            if(x <= v[res])
                cmp = true;
        }

        return res;
    }

    private void invertir(int[] v)
    {
        int aux;
        for(int i=0;i<v.length/2;i++)
        {
            aux = v[i];
            v[i] = v[v.length-i-1];
            v[v.length-i-1]=aux;
        }
    }

    private Integer[] ActualizarMejor(int[][] pob)
    {
        int fit[] = new int[this.individuos*2];
        Integer[] indices = new Integer[this.individuos*2];

        for(int i=0;i<this.individuos;i++)
        {
            fit[i] = this.fitness.apply(this.poblacion[i]);
            fit[i+this.individuos] = this.fitness.apply(pob[i]);
            indices[i] = i;
            indices[i+this.individuos] = i+this.individuos;
        }

        Arrays.sort(indices, (a, b) -> Integer.compare(fit[a], fit[b]));

        if(indices[0]<this.individuos)
            this.bestSol = Arrays.copyOf(this.poblacion[indices[0]], this.dimension);
        else
            this.bestSol = Arrays.copyOf(pob[indices[0]-this.individuos],this.dimension);

        this.bestValue = this.fitness.apply(this.bestSol);

        return indices;
    }
}
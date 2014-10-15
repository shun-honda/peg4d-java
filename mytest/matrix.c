#include <stdio.h>
#include <math.h>

#define N 2
#define Pi 3.14159

void Mul(double x[][N], double y[][N], double z[][N]);
void Copy(double x[][N], double y[][N]);
void Display(double x[][N]);


void main(){
    double x[N][N] = {{1.0,0.0},{0.0, 1.0}}, a[N][N], c[N][N];
    int i;

    a[0][0] = cos(2 * Pi/100);   a[0][1] = - sin(2 * Pi/100);
    a[1][0] = sin(2 * Pi/100);   a[1][1] =   cos(2 * Pi/100);

    for(i = 0; i<100; i++){
        Mul(x, a, c);
        Copy(c, x);
    }

    Display(x);

}

void Mul(double x[][N], double y[][N], double z[][N]){
    int i,j,k;

    for(i = 0; i<N; i++){
        for(j = 0; j<N; j++){
            z[i][j] = 0.0;
            for(k = 0; k<N; k++){
                z[i][j] = z[i][j] + x[i][k] * y[k][j];
            }
        }
    }
}

void Copy(double x[][N], double y[][N]){
    int i,j;

    for(i = 0; i<N; i++){
        for(j = 0; j<N; j++){
            y[i][j] = x[i][j];
        }
    }
}

void Display(double x[][N]){
    int i, j;
    
    for(i = 0; i<N; i++){
        for(j = 0; j<N; j++){
            printf("%f ", x[i][j]); 
        }
        printf("\n");
    }

}
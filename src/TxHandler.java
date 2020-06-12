import java.util.ArrayList;

public class TxHandler {

    // Properties
    UTXOPool coinPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        coinPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *      *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {

        //Variable to calculate the amount of coin exists by inputs
        double sumInputs = 0;

        //Arraylist for condition 3 to check if a coin is just used once
        ArrayList<UTXO> usedCoins = new ArrayList<>();

        for( int counter = 0; counter < tx.numInputs(); counter++){
            //Get the input
            byte[] prevTxHash = tx.getInput(counter).prevTxHash;
            byte[] signature = tx.getInput(counter).signature;
            int outputIndex = tx.getInput(counter).outputIndex;

            UTXO inputCoin = new UTXO( prevTxHash, outputIndex);

            // Condition 1 - The coin in the input must be in the pool to be spent
            if ( ! coinPool.contains(inputCoin) )
                return false;

            // Condition 2 - Public key, message and signature are needed to check if the input is valid
            byte[] message = tx.getRawDataToSign( counter );

            if ( ! Crypto.verifySignature( coinPool.getTxOutput(inputCoin).address, message, signature) )
                return false;

            // Condition 3 - It will return false if UTXO is used before
            if( usedCoins.contains(inputCoin) )
                return false;

            sumInputs += coinPool.getTxOutput( inputCoin ).value;
            usedCoins.add( inputCoin );

        }

        //Variable to compute the amount of coin that are needed by outputs for transaction
        double sumOutputs = 0;

        //Condition 4 - Checks if there exists a non-negative outputs because balance must not go below zero and computes the sum of outputs
        for ( int counter = 0; counter < tx.numOutputs(); counter++ ){
            if ( tx.getOutput( counter ).value < 0 )
                return false;
            else
                sumOutputs += tx.getOutput( counter ).value;
        }

        //Condition 5 - Checks if the sum of outputs(wanted coins for tx) is less than inputs(balance).
        //However, I did not understand why sum of outputs and inputs can't be equal to each other.
        if( sumInputs < sumOutputs )
            return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        //Array of transactions
        ArrayList<Transaction> validTxs = new ArrayList<>();

        //Go trough all transactions
        for( int counter = 0; counter < possibleTxs.length; counter++ ){
            // If current transaction is valid
            if ( isValidTx(possibleTxs[counter]) ){

                validTxs.add(possibleTxs[counter]);

                //Clearing the pool
                for( int inputCounter = 0; inputCounter < possibleTxs[counter].numInputs(); inputCounter++ ){
                    byte[] prevTxHash = possibleTxs[counter].getInput(inputCounter).prevTxHash;
                    int outputIndex = possibleTxs[counter].getInput(inputCounter).outputIndex;

                    UTXO inputCoin = new UTXO( prevTxHash, outputIndex);

                    coinPool.removeUTXO( inputCoin );
                }

                //Adding coins back
                for( int outputCounter = 0; outputCounter < possibleTxs[counter].numOutputs(); outputCounter++ ){

                    UTXO inputCoin = new UTXO( possibleTxs[counter].getHash(), outputCounter);

                    coinPool.addUTXO( inputCoin, possibleTxs[counter].getOutput(outputCounter));
                }

            }
        }

        Transaction[] validTxsArr = new Transaction[validTxs.size()];
        validTxsArr = validTxs.toArray(validTxsArr);
        return validTxsArr;
    }

}

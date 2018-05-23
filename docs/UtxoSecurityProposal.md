# UTXO security without maintaining a full set of outputs

## Abstract

This paper is devoted to the study of UTXO maintaining in massively replicated open blockchain systems. 
In such systems, like Bitcoin, a snapshot of current state required for the validation of transactions is being held in 
the memory, which eventually becomes a scarce resource. Uncontrolled state growth can lead to security issues. We propose 
a modification of UTXO model and also transaction validation algorithm.

## UOHS (Unspent Output Hashes Set)

The idea is to store in UTXO only hashes of unspent outputs rather than their full versions. Meanwhile, the transaction 
trying to spend some output should contain its full version for validation purposes. This allows to significantly decrease 
the amount of data required to be stored in UTXO which reside in expensive random access memory (RAM). Tha main drawback 
of such approach is an increase of network load.

## Research

To find out whether the advantages of such approach overweight the drawbacks we should ...

    Transaction length:    264
    Outputs lengths:       Vector(57, 57, 20)
    Outputs lengths total: 134

Current transaction structure:

    senderPubKey;   32 bytes
    signature;      64 bytes
    timestamp;       8 bytes
    fee;             8 bytes
    inputs;         32 bytes / each
    outputs;       ~46 bytes / each

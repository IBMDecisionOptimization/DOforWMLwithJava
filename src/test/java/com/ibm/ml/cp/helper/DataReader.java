package com.ibm.ml.cp.helper;

import ilog.concert.IloException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;

public class DataReader {

	private StreamTokenizer st;

	public DataReader(String filename) {
		InputStream input = getClass().getClassLoader().getResourceAsStream(filename);
		Reader r = new BufferedReader(new InputStreamReader(input));
		st = new StreamTokenizer(r);
	}

	public int next() throws IOException {
		st.nextToken();
		return (int) st.nval;
	}
}

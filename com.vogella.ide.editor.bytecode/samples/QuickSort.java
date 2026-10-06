package com.example.sort;

import java.util.Arrays;

/** Sorts an int array in place with the quicksort algorithm. */
public class QuickSort {

	public static void sort(int[] values) {
		quickSort(values, 0, values.length - 1);
	}

	private static void quickSort(int[] values, int low, int high) {
		if (low < high) {
			int pivotIndex = partition(values, low, high);
			quickSort(values, low, pivotIndex - 1);
			quickSort(values, pivotIndex + 1, high);
		}
	}

	private static int partition(int[] values, int low, int high) {
		int pivot = values[high];
		int i = low - 1;
		for (int j = low; j < high; j++) {
			if (values[j] <= pivot) {
				i++;
				swap(values, i, j);
			}
		}
		swap(values, i + 1, high);
		return i + 1;
	}

	private static void swap(int[] values, int i, int j) {
		int temp = values[i];
		values[i] = values[j];
		values[j] = temp;
	}

	public static void main(String[] args) {
		int[] values = { 42, 7, 19, 3, 88, 23, 1, 56 };
		sort(values);
		System.out.println(Arrays.toString(values));
	}
}

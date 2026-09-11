package com.jarvis.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/** Safe arithmetic evaluator (shunting-yard, no eval()).
 *  Supports + - * / % ^ ( ) and negative numbers, e.g. "2+2*10", "(5+3)^2", "2*-3". */
public class Calc {
    public static String eval(String expr) {
        try {
            List<String> t = tokenize(expr);
            if (t.isEmpty()) return null;
            double v = rpn(t);
            if (Double.isNaN(v) || Double.isInfinite(v)) return null;
            if (v == Math.floor(v) && Math.abs(v) < 1e15) return String.valueOf((long) v);
            return String.valueOf(Math.round(v * 1e6) / 1e6);
        } catch (Exception e) {
            return null;
        }
    }

    private static List<String> tokenize(String s) {
        List<String> out = new ArrayList<String>();
        int i = 0, n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                out.add(s.substring(i, j));
                i = j;
                continue;
            }
            if (c == '-' && (out.isEmpty() || isOp(out.get(out.size() - 1)) || out.get(out.size() - 1).equals("("))) {
                int j = i + 1;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) j++;
                if (j > i + 1) { out.add(s.substring(i, j)); i = j; continue; }
            }
            out.add(String.valueOf(c));
            i++;
        }
        return out;
    }

    private static boolean isOp(String t) {
        return t.equals("+") || t.equals("-") || t.equals("*") || t.equals("/")
                || t.equals("%") || t.equals("^") || t.equals("(");
    }

    private static int prec(String op) {
        if (op.equals("^")) return 3;
        if (op.equals("*") || op.equals("/") || op.equals("%")) return 2;
        if (op.equals("+") || op.equals("-")) return 1;
        return 0;
    }

    private static double rpn(List<String> tokens) {
        Stack<Double> vals = new Stack<Double>();
        Stack<String> ops = new Stack<String>();
        for (String t : tokens) {
            if (isNumber(t)) { vals.push(Double.parseDouble(t)); continue; }
            if (t.equals("(")) { ops.push(t); continue; }
            if (t.equals(")")) {
                while (!ops.isEmpty() && !ops.peek().equals("(")) apply(vals, ops.pop());
                if (!ops.isEmpty()) ops.pop();
                continue;
            }
            while (!ops.isEmpty() && !ops.peek().equals("(")
                    && (prec(ops.peek()) > prec(t) || (prec(ops.peek()) == prec(t) && !t.equals("^")))) {
                apply(vals, ops.pop());
            }
            ops.push(t);
        }
        while (!ops.isEmpty()) apply(vals, ops.pop());
        return vals.isEmpty() ? 0 : vals.pop();
    }

    private static boolean isNumber(String t) {
        if (t.isEmpty()) return false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (!Character.isDigit(c) && c != '.' && !(c == '-' && i == 0)) return false;
        }
        return true;
    }

    private static void apply(Stack<Double> vals, String op) {
        double b = vals.isEmpty() ? 0 : vals.pop();
        double a = vals.isEmpty() ? 0 : vals.pop();
        if (op.equals("+")) vals.push(a + b);
        else if (op.equals("-")) vals.push(a - b);
        else if (op.equals("*")) vals.push(a * b);
        else if (op.equals("/")) vals.push(a / b);
        else if (op.equals("%")) vals.push(a % b);
        else if (op.equals("^")) vals.push(Math.pow(a, b));
    }
}

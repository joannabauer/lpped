/*
 * Copyright (C) 2012 Andreas Halle
 *
 * This file is part of lpped.
 *
 * lpped is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * lpped is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public license
 * along with lpped. If not, see <http://www.gnu.org/licenses/>.
 */
package controller;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import model.LP;
import model.Matrix;

class Parser {
    final static String dvarreg =
        "\\s*([\\+\\-])?\\s*(\\d+(?:\\.\\d+)?)?\\s*([a-z]+\\d*)";
    final static String objreg =
        String.format("^(?:maximize|max)(?:%s)+", dvarreg);
    final static String conreg =
        String.format("^(?:subject to|s\\.t\\.)?(?:%s)+\\s*[<=]{1,2}\\s*([\\"
                + "+\\-])?\\s*(\\d+(?:\\.\\d+)?)?", dvarreg);



    static boolean validObj(String objective) {
        return objective.matches(objreg);
    }



    static boolean validConstraint(String constraint) {
        return constraint.matches(conreg);
    }



    static boolean validVarName(String varName) {
        String varLow = varName.toLowerCase();
        return   !(varLow.equals("max")
                || varLow.equals("maximize")
                || varLow.equals("subject")
                || varLow.equals("to")
                || varLow.equals("s.t."));
    }



    static LP parse(File f) throws FileNotFoundException {
        Scanner s = new Scanner(f);
        Pattern p = Pattern.compile(dvarreg);

        HashMap<String, Integer> x = new HashMap<String, Integer>();
        HashMap<Integer, String> xReverse = new HashMap<Integer, String>();
        int xcol = 0;

        /* Get input size and names of the decision variables. */
        int constraints = -1; // Take the objective function into account.
        while (s.hasNextLine()) {
            String line = s.nextLine();

            /* 
             * TODO: Beware, will now accept invalid
             * files with multiple objective functions.
             */
/*            if (!validConstraint(line) && !validObj(line)) {
                String e = "Unsupported format in file " + f;
                throw new IllegalArgumentException(e);
            } */

            Matcher m = p.matcher(line);

            while (m.find()) {
                String var = m.group(3);
                if (validVarName(var) && !x.containsKey(var)) {
                    x.put(var, xcol);
                    xReverse.put(xcol++, var);
                }
            }
            constraints++;
        }

        double[][] Ndata = new double[constraints][x.size()];
        double[] bdata = new double[constraints];
        double[] cdata = new double[x.size()];

        s = new Scanner(f);

        String obj = s.nextLine();
        Matcher m = p.matcher(obj);

        while (m.find()) {
            String var = m.group(3);
            if (!x.containsKey(var)) continue;

            String sign = m.group(1);
            if (sign == null) sign = "+";

            String coeffStr = m.group(2);
            double coeff =
                (coeffStr == null) ? 1 : Double.parseDouble(coeffStr);
            if (sign.equals("-")) coeff = -coeff;

            cdata[x.get(var)] = coeff;
        }

        int row = 0;
        while (s.hasNextLine()) {
            String line = s.nextLine();
            m = p.matcher(line);
            bdata[row] = Double.parseDouble(line.split("<=")[1]);

            while (m.find()) {
                String var = m.group(3);
                if (!x.containsKey(var)) continue;

                String sign = m.group(1);
                if (sign == null) sign = "+";

                String coeffStr = m.group(2);
                double coeff =
                    (coeffStr == null) ? 1 : Double.parseDouble(coeffStr);
                if (sign.equals("-")) coeff = -coeff;

                Ndata[row][x.get(var)] = coeff;
            }
            row++;
        }

        return new LP(new Matrix(Ndata), new Matrix(bdata).transpose(),
                                      new Matrix(cdata).transpose(), xReverse);
    }
}

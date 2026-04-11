import { NextRequest, NextResponse } from 'next/server';
import fs from 'fs';
import path from 'path';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const { name, id, value, rating, navigationType } = body;

    const logDir = path.join(process.cwd(), '../../logs');
    const logFilePath = path.join(logDir, 'web-vitals.log');

    if (!fs.existsSync(logDir)) {
      fs.mkdirSync(logDir, { recursive: true });
    }

    const logEntry = {
      timestamp: new Date().toISOString(),
      name,
      id,
      value,
      rating,
      navigationType,
      userAgent: req.headers.get('user-agent'),
    };

    fs.appendFileSync(logFilePath, JSON.stringify(logEntry) + '\n');

    return NextResponse.json({ message: 'Web Vitals received' }, { status: 200 });
  } catch (error) {
    console.error('Error logging web vitals:', error);
    return NextResponse.json({ message: 'Error logging web vitals' }, { status: 500 });
  }
}

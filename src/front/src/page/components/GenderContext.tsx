import React, { createContext, useState, ReactNode } from 'react';

interface GenderContextProps {
    genderFilter: string;
    setGenderFilter: (gender: string) => void;
}

export const GenderContext = createContext<GenderContextProps>({
    genderFilter: 'all',
    setGenderFilter: () => {},
});

interface GenderProviderProps {
    children: ReactNode;
}

export const GenderProvider: React.FC<GenderProviderProps> = ({ children }) => {
    const [genderFilter, setGenderFilter] = useState<string>('all');

    return (
        <GenderContext.Provider value={{ genderFilter, setGenderFilter }}>
            {children}
        </GenderContext.Provider>
    );
};